package io.hhplus.tdd.point.service;

import java.util.List;
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

	private final Lock lock = new ReentrantLock();

	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		lock.lock();  // 락 획득
		try {
			UserPoint userPoint = selectUserPoint(userId);
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);
			recordPointHistory(updatedUserPoint, type);
			return updatedUserPoint;
		} finally {
			lock.unlock();  // 락 해제
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
	}

	private synchronized UserPoint updateUserPoints(long id, long amount) {
		return pointRepository.insertOrUpdate(id, amount);
	}

	private synchronized void recordPointHistory(UserPoint updateUserPoint, TransactionType type) {
		pointRepository.pointHistoryInsert(updateUserPoint, type);
	}

	public UserPoint selectUserPoint(long userId) {
		return pointRepository.selectUserPoint(userId);
	}

	public List<PointHistory> selectUserPointHistory(long userId) {
		return pointRepository.selectUserPointHistory(userId);
	}
}
