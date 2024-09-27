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

	private final PointRepository pointRepository;
	private final Queue<PointHistoryReTry> requestQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();
	private static final long INITIAL_RETRY_DELAY = 10; // 초기 지연 시간 (초)
	private static final long MAX_RETRY_DELAY = 600; // 최대 지연 시간 (10분)
	private static final int MAX_RETRY_ATTEMPTS = 5; // 최대 재시도 횟수 설정
	private static final long INITIAL_SCHEDULER_DELAY = 20; // 스케줄러 초기 지연 시간 (초)

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // 스케줄러 생성

	private static final Logger logger = LoggerFactory.getLogger(PointService.class);

	@Autowired
	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
		startRetryScheduler(); // 스케줄러 시작
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
			// 포인트 조회 및 업데이트 부분만 락으로 보호
			UserPoint userPoint = selectUserPoint(userId);
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);
			recordPointHistory(updatedUserPoint, type);

			return updatedUserPoint;
		} finally {
			lock.unlock(); // 락 해제
		}
	}

	// 포인트 사용
	public UserPoint useUserPoints(long userId, long amount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			// 포인트 조회
			UserPoint userPoint = selectUserPoint(userId);

			// 포인트가 부족한 경우 예외 발생 및 큐에 요청 저장
			if (userPoint.point() < amount) {
				long threadId = Thread.currentThread().getId(); // 스레드 ID를 가져와서 사용
				PointHistoryReTry failedRequest = new PointHistoryReTry(threadId, userId, amount, type, System.currentTimeMillis(), 0);
				requestQueue.offer(failedRequest);
				logger.info("유저 [{}]의 포인트 부족으로 요청 큐에 추가: {} 포인트, 스레드 ID: {}", userId, amount, threadId);
				throw new IllegalArgumentException("사용할 포인트가 없습니다.");
			}

			// 포인트 사용
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);
			recordPointHistory(UserPoint.changePoint(updatedUserPoint, amount), type);

			return updatedUserPoint;
		} finally {
			lock.unlock();  // 락 해제
		}
	}

	// 포인트 재시도 스케줄러 시작
	private void startRetryScheduler() {
		scheduler.scheduleAtFixedRate(this::processPendingRequests, INITIAL_SCHEDULER_DELAY, 10, TimeUnit.SECONDS);
		logger.info("포인트 재시도 스케줄러가 {}초 후 시작됩니다.", INITIAL_SCHEDULER_DELAY); // 스케줄러가 시작될 때 로그 출력
	}


	// 큐에 저장된 실패한 요청을 재시도
	public void processPendingRequests() {
		logger.info("재시도 스케줄러 실행 중. 큐 상태: {}건", requestQueue.size());  // 큐 크기 확인 로그

		if (requestQueue.isEmpty()) {
			logger.info("처리할 실패한 요청이 없습니다.");
			return;
		}

		PointHistoryReTry request = requestQueue.poll();
		if (request != null) {
			try {
				logger.info("유저 [{}]의 포인트 사용 요청 처리 중: {} 포인트, 스레드 ID: {}, 재시도 횟수 {}", request.getUserId(), request.getAmount(), request.getId(), request.getRetryCount());
				useUserPoints(request.getUserId(), request.getAmount(), request.getType());
			} catch (IllegalArgumentException e) {
				logger.error("유저 [{}]의 포인트 사용 실패, 요청 재시도: {} 포인트, 스레드 ID: {}, 재시도 횟수 {}", request.getUserId(), request.getAmount(), request.getId(), request.getRetryCount());

				if (request.getRetryCount() < MAX_RETRY_ATTEMPTS) {
					// 재시도 횟수를 증가시키는 불변 객체 생성
					PointHistoryReTry updatedRequest = request.incrementRetryCount();
					long delay = calculateExponentialBackoffDelay((int) updatedRequest.getRetryCount());
					scheduler.schedule(() -> requestQueue.offer(updatedRequest), delay, TimeUnit.SECONDS);
				} else {
					logger.warn("유저 [{}]의 요청이 최대 재시도 횟수를 초과했습니다: {} 포인트, 스레드 ID: {}", request.getUserId(), request.getAmount(), request.getId());
				}
			}
		}
	}


	// 지수적 백오프 지연 시간 계산
	private long calculateExponentialBackoffDelay(int retryCount) {
		return Math.min(INITIAL_RETRY_DELAY * (long) Math.pow(2, retryCount), MAX_RETRY_DELAY);
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
