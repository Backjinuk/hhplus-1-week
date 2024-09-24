package io.hhplus.tdd.point;

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
		UserPoint userPoint = selectUserPoint(userId);

		// 포인트 합산 및 업데이트
		UserPoint updateUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);

		// 포인트 히스토리 기록
		recordPointHistory(updateUserPoint, type);

		return updateUserPoint;
	}

	public UserPoint useUserPoints(long userId, int amount, TransactionType type) {
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
}
