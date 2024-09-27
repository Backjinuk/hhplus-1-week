package io.hhplus.tdd.point.service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.PointHistoryReTry;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.repository.PointRepository;

@Service
public class PointService {

	/*
	 *
	 * 여러 유저가 동시에 포인트를 사용하거나 충전할 때 동시성 문제를 해결하기 위해
	 * 유저별로 ReentrantLock을 사용하여 동시성을 제어.
	 *
	 * 포인트가 부족한 경우는 여러 스레드가 동시에 포인트를 사용하려 할 때 발생할 수 있으며,
	 * 이를 방지하기 위해 예외 처리 후 해당 요청을 큐에 저장.
	 *
	 * 큐에 저장된 요청은 스케줄러를 통해 일정 시간 후에 재시도하며,
	 * 지수적 백오프 방식을 적용하여 서버 부하를 줄이며 순차적으로 요청을 처리.
	 *
	 */

	private final PointRepository pointRepository;
	private final Queue<PointHistoryReTry> requestQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();
	private static final long INITIAL_RETRY_DELAY = 10; // 초기 지연 시간 (초)
	private static final long MAX_RETRY_DELAY = 60; // 최대 지연 시간 (10분)
	private static final int MAX_RETRY_ATTEMPTS = 5; // 최대 재시도 횟수 설정

	private ScheduledExecutorService scheduler;
	private static final Logger logger = LoggerFactory.getLogger(PointService.class);

	@Autowired
	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
	}

	// 유저별 Lock을 관리하는 ConcurrentHashMap
	private Lock getUserLock(long userId) {
		return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
	}

	// 포인트 충전
	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			return applyPointTransaction(userId, chargeAmount, type);
		} finally {
			lock.unlock();
		}
	}

	// 포인트 사용
	public UserPoint useUserPoints(long userId, long amount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			UserPoint userPoint = selectUserPoint(userId);

			if (userPoint.point() < amount) {
				enqueueRetryRequest(userId, amount, type);
				throw new IllegalArgumentException("사용할 포인트가 없습니다.");
			}

			return applyPointTransaction(userId, -amount, type);
		} finally {
			lock.unlock();

		}
	}

	// 포인트 충전/차감 처리
	private UserPoint applyPointTransaction(long userId, long amount, TransactionType type) {
		UserPoint userPoint = selectUserPoint(userId);
		UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + amount);
		recordPointHistory(updatedUserPoint, type);
		return updatedUserPoint;
	}

	// 큐에 실패한 요청을 저장하고 스케줄러 시작
	private void enqueueRetryRequest(long userId, long amount, TransactionType type) {
		startScheduler();

		long threadId = Thread.currentThread().getId();
		PointHistoryReTry failedRequest = new PointHistoryReTry(threadId, userId, amount, type,
			System.currentTimeMillis(), 0);
		requestQueue.offer(failedRequest);

		logger.info("유저 [{}]의 포인트 부족으로 요청 큐에 추가: {} 포인트, 스레드 ID: {}", userId, amount, threadId);
	}

	// 큐에 저장된 실패한 요청을 재시도
	public void processPendingRequests() {
		if (requestQueue.isEmpty()) {
			logger.info("처리할 실패한 요청이 없습니다.");
			stopScheduler();
			return;
		}

		PointHistoryReTry request = requestQueue.poll();
		if (request != null) {
			processSingleRequest(request);
		}
	}

	// 개별 요청 처리 및 실패 시 재시도
	private void processSingleRequest(PointHistoryReTry request) {
		try {
			logger.info("유저 [{}]의 포인트 사용 요청 처리 중: {} 포인트, 스레드 ID: {}, 재시도 횟수 {}", request.getUserId(),
				request.getAmount(), request.getId(), request.getRetryCount());
			useUserPoints(request.getUserId(), request.getAmount(), request.getType());
		} catch (IllegalArgumentException e) {
			handleFailedRequest(request);
		}
	}

	// 실패한 요청 처리
	private void handleFailedRequest(PointHistoryReTry request) {
		logger.error("유저 [{}]의 포인트 사용 실패, 요청 재시도: {} 포인트, 스레드 ID: {}, 재시도 횟수 {}", request.getUserId(),
			request.getAmount(), request.getId(), request.getRetryCount());

		if (request.getRetryCount() < MAX_RETRY_ATTEMPTS) {
			PointHistoryReTry updatedRequest = request.incrementRetryCount();
			long delay = calculateExponentialBackoffDelay((int)updatedRequest.getRetryCount());
			scheduler.schedule(() -> requestQueue.offer(updatedRequest), delay, TimeUnit.SECONDS);

			logger.info("유저 [{}]의 요청이 다시 큐에 추가되었습니다. 재시도 횟수: {}", updatedRequest.getUserId(),
				updatedRequest.getRetryCount());
		} else {
			logger.warn("유저 [{}]의 요청이 최대 재시도 횟수를 초과했습니다: {} 포인트, 스레드 ID: {}", request.getUserId(),
				request.getAmount(), request.getId());
		}
	}

	// 지수적 백오프 지연 시간 계산
	private long calculateExponentialBackoffDelay(int retryCount) {
		return Math.min(INITIAL_RETRY_DELAY * (long)Math.pow(2, retryCount), MAX_RETRY_DELAY);
	}

	// 스케줄러를 시작하는 메서드
	public void startScheduler() {
		if (scheduler != null && !scheduler.isShutdown()) {
			logger.warn("스케줄러는 이미 실행 중입니다.");
			return;
		}

		scheduler = Executors.newScheduledThreadPool(1);
		// 지정된 시간 간격으로 반복적으로 processPendingRequests 를 실행
		scheduler.scheduleAtFixedRate(this::processPendingRequests, 0, 20, TimeUnit.SECONDS);
		logger.info("포인트 재시도 스케줄러가 즉시 시작됩니다.");
	}

	// 스케줄러를 중지하는 메서드
	public void stopScheduler() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			logger.info("포인트 재시도 스케줄러가 중지되었습니다.");
		} else {
			logger.warn("스케줄러가 실행 중이지 않습니다.");
		}
	}

	private UserPoint updateUserPoints(long id, long amount) {
		return pointRepository.insertOrUpdate(id, amount);
	}

	private void recordPointHistory(UserPoint updateUserPoint, TransactionType type) {
		pointRepository.pointHistoryInsert(updateUserPoint, type);
	}

	public UserPoint selectUserPoint(long userId) {
		return pointRepository.selectUserPoint(userId);
	}

	public List<PointHistory> selectUserPointHistory(long userId) {
		return pointRepository.selectUserPointHistory(userId);
	}
}
