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

	private final PointRepository pointRepository;

	@Autowired
	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
	}
/*
* 동시성 문제는 해결되었지만 순차적으로 스레드가 실행되지 않아 문제가 발생
*
* 오류가 발생한 부분을 queu에 넣고 다시 호출하는식 대안으로 생각함
* */

	private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();
	private final Queue<PointHistory> failedRequests = new ConcurrentLinkedQueue<PointHistory>();


	// 유저별 Lock을 관리하는 ConcurrentHashMap
	private Lock getUserLock(long userId) {
		return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
	}


	// 포인트 충전
	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			// 포인트 조회 및 업데이트
			UserPoint userPoint = selectUserPoint(userId);
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);
			recordPointHistory(updatedUserPoint, type);

			// 큐에 있는 실패한 요청을 재시도
			retryFailedRequests(userId);

			return updatedUserPoint;
		} finally {
			lock.unlock();
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
				failedRequests.offer(new PointHistory(0, userId, amount, type, 0));
				throw new IllegalArgumentException("사용할 포인트가 없습니다.");
			}

			// 포인트 사용
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);
			recordPointHistory(UserPoint.changePoint(updatedUserPoint, amount), type);

			return updatedUserPoint;
		} finally {
			lock.unlock();
		}
	}

	// 큐에 저장된 실패한 요청을 재시도
	private void retryFailedRequests(long userId) {
		PointHistory failedRequest;
		while ((failedRequest = failedRequests.poll()) != null) {
			if (failedRequest.userId() == userId) {
				try {
					// 재시도: 실패한 포인트 사용 요청 다시 시도
					useUserPoints(failedRequest.userId(), failedRequest.amount(), failedRequest.type());
				} catch (IllegalArgumentException e) {
					// 여전히 포인트가 부족할 경우 다시 큐에 저장
					//failedRequests.offer(failedRequest);
				}
			} else {
				// 다른 유저의 요청은 다시 큐에 넣음
				failedRequests.offer(failedRequest);
			}
		}
	}

	private  UserPoint updateUserPoints(long id, long amount) {
		return pointRepository.insertOrUpdate(id, amount);
	}

	private  void recordPointHistory(UserPoint updateUserPoint, TransactionType type) {
		pointRepository.pointHistoryInsert(updateUserPoint, type);
	}

	public UserPoint selectUserPoint(long userId) {
		return pointRepository.selectUserPoint(userId);
	}

	public List<PointHistory> selectUserPointHistory(long userId) {
		return pointRepository.selectUserPointHistory(userId);
	}


}
/*
 * synchronized는 서비스 객체에서 동시성을 제어 하므로 여러 유저가 동시에 포인트를 사용하거나 충전할때 동시성 문제발생
 * 유저별로 lock을 걸어서 동시성을 관리하는 ReentrantLock을 사용
 *



public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
	synchronized (this) {
		// 포인트 조회 및 업데이트 부분만 동기화
		UserPoint userPoint = selectUserPoint(userId);
		UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);
		recordPointHistory(updatedUserPoint, type);
		return updatedUserPoint;
	}
}


public UserPoint useUserPoints(long userId, long amount, TransactionType type) {

	synchronized (this) {
		UserPoint userPoint = selectUserPoint(userId);

		if (userPoint.point() == 0 || userPoint.point() < amount) {
			throw new IllegalArgumentException("사용할 포인트가 없습니다.");
		}

		UserPoint useUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);

		recordPointHistory(UserPoint.changePoint(useUserPoint, amount), type);

		return useUserPoint;
	}
}*/
