package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PointService {

	private final PointRepository pointRepository;

	@Autowired
	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
	}

	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		//기존 포인트 조회 OR 기본 포인트 생성
		UserPoint userPoint = pointRepository.selectById(userId);

		// 포인트 합산 및 업데이트
		UserPoint updateUserPoint = updateUserPoints(userPoint, chargeAmount);

		// 포인트 히스토리 기록
		recordPointHistory(updateUserPoint, type);

		return updateUserPoint;
	}


	private UserPoint updateUserPoints(UserPoint userPoint, long chargeAmount) {
		long updateAmount = userPoint.point() + chargeAmount;
		return pointRepository.insertOrUpdate(userPoint.id(), updateAmount);
	}


	private void recordPointHistory(UserPoint updateUserPoint, TransactionType type) {
		pointRepository.pointHistoryInsert(updateUserPoint, type);
	}
}
