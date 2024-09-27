package io.hhplus.tdd.point.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;

@Repository
public class PointRepositoryImpl implements PointRepository {

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	@Autowired
	public PointRepositoryImpl(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
		this.userPointTable = userPointTable;
		this.pointHistoryTable = pointHistoryTable;
	}

	@Override
	public UserPoint insertOrUpdate(long id, long amount) {
		return userPointTable.insertOrUpdate(id, amount);
	}

	@Override
	public void pointHistoryInsert(UserPoint userPoint, TransactionType type) {
		pointHistoryTable.insert(userPoint.id(), userPoint.point(), type, userPoint.updateMillis());
	}

	@Override
	public UserPoint selectUserPoint(long userId) {
		return userPointTable.selectById(userId);
	}

	@Override
	public UserPoint useUserPoints(long userId, long point) {
		return userPointTable.insertOrUpdate(userId, point);
	}

	@Override
	public List<PointHistory> selectUserPointHistory(long userId) {
		return pointHistoryTable.selectAllByUserId(userId);
	}
}
