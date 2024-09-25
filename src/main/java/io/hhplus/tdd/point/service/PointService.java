package io.hhplus.tdd.point.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.repository.PointRepository;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;

@Service
public class PointService {

	private final PointRepository pointRepository;

	@Autowired
	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
	}

	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		//기존 포인트 조회 OR 기본 포인트 생성
		UserPoint userPoint = selectUserPoint(userId);

		// 포인트 합산 및 업데이트
		UserPoint updateUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);

		// 포인트 히스토리 기록
		recordPointHistory(updateUserPoint, type);

		return updateUserPoint;
	}

	public UserPoint useUserPoints(long userId, long amount, TransactionType type) {
		UserPoint userPoint = selectUserPoint(userId);

		if (userPoint.point() == 0 || userPoint.point() < amount) {
			throw new IllegalArgumentException("사용할 포인트가 없습니다.");
		}

		UserPoint useUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);

		recordPointHistory(UserPoint.changePoint(useUserPoint, amount), type);

		return useUserPoint;
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

	public List<PointHistory> selectUserPointHistory(long userId) { return pointRepository.selectUserPointHistory(userId);
}
}
