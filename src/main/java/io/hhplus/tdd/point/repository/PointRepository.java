package io.hhplus.tdd.point.repository;

import java.util.List;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;

public interface PointRepository {

	UserPoint insertOrUpdate(long id, long amount);

	void pointHistoryInsert(UserPoint insertOrUpdateUserPoint, TransactionType type);

	UserPoint selectUserPoint(long userId);

	UserPoint useUserPoints(long userId, long point);

	List<PointHistory> selectUserPointHistory(long userId);
}
