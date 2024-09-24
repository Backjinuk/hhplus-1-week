package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@Repository
public class PointRepositoryImpl implements PointRepository{

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	@Autowired
	public PointRepositoryImpl(UserPointTable userPointTable, PointHistoryTable pointHistoryTable){
		this.userPointTable = userPointTable;
		this.pointHistoryTable = pointHistoryTable;
	}

	@Override
	public UserPoint selectById(long id) {
		return userPointTable.selectById(id);
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
	public List<PointHistory> selectAllByUserId(long id) {
		return pointHistoryTable.selectAllByUserId(id);
	}
}
