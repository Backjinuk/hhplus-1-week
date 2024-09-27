package io.hhplus.tdd.point.service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.repository.PointRepository;

@Service
public class PointService {

	/*
	 *
	 * synchronized는 서비스 객체에서 동시성을 제어 하므로 여러 유저가 동시에 포인트를 사용하거나 충전할때 동시성 문제발생
	 * 유저별로 lock을 걸어서 동시성을 관리하는 ReentrantLock을 사용
	 *
	 * 동시성 문제는 해결되었지만 순차적으로 스레드가 실행되지 않아 문제가 발생
	 * 오류가 발생한 부분을 queu에 넣고 다시 호출하는식 대안으로 생각함
	 *
	 * */

	private final PointRepository pointRepository;
	private final Queue<PointHistory> failedRequests = new ConcurrentLinkedQueue<PointHistory>();
	private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

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
				failedRequests.offer(new PointHistory(0, userId, amount, type, 1));
				// System.out.println("포인트 부족으로 큐에 저장된 요청: " + failedRequests); // 큐에 저장될 때 콘솔 출력
				throw new IllegalArgumentException("사용할 포인트가 없습니다.");
			}

			// 포인트 사용
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);
			recordPointHistory(UserPoint.changePoint(updatedUserPoint, amount), type);

			return updatedUserPoint;
		} finally {
			lock.unlock();

			// 큐에 있는 실패한 요청을 재시도
			retryFailedRequests(userId);
		}
	}

	// 큐에 저장된 실패한 요청을 재시도
	private void retryFailedRequests(long userId) {
		Queue<PointHistory> localQueue = new ConcurrentLinkedQueue<>();

		// 유저 ID와 일치하는 실패 요청만 처리
		for (PointHistory failedRequest : failedRequests) {
			if (failedRequest.userId() == userId) {
				try {

					long point = pointRepository.selectUserPoint(userId).point();

					if(point > failedRequest.amount()){

						// 포인트 사용 재시도
						useUserPoints(failedRequest.userId(), failedRequest.amount(), failedRequest.type());
					}
				} catch (IllegalArgumentException e) {
					// 포인트 부족으로 재시도 실패
					System.err.println("포인트 부족: UserId = " + userId);
					localQueue.offer(failedRequest); // 실패한 요청은 다시 로컬 큐에 넣음
				}
			} else {
				localQueue.offer(failedRequest); // 다른 유저의 요청은 로컬 큐에 유지
			}
		}

		failedRequests.clear(); // 기존 큐 비우기
		failedRequests.addAll(localQueue); // 처리되지 않은 요청 다시 삽입
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
