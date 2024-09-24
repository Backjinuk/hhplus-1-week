package io.hhplus.tdd.point;

import java.util.List;

public interface PointRepository  {

	UserPoint selectById(long id);

	UserPoint insertOrUpdate(long id, long amount);

	void pointHistoryInsert(UserPoint insertOrUpdateUserPoint, TransactionType type);

	List<PointHistory> selectAllByUserId(long id);
}
