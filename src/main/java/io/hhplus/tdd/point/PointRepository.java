package io.hhplus.tdd.point;

import java.util.List;

public interface PointRepository {

	UserPoint selectById(long id);

	UserPoint insertOrUpdate(long id, long amount);

	void pointHistoryInsert(UserPoint insertOrUpdateUserPoint, TransactionType type);

	List<PointHistory> selectAllByUserId(long id);

	UserPoint selectUserPoint(long userId);

	UserPoint useUserPoints(long userId, long point);
}
