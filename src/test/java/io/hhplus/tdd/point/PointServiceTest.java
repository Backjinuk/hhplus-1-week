package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PointServiceTest {

	@Autowired
	PointService pointService;

	@Autowired
	PointRepository pointRepository;

	/*
	 * getUserPoints()
	 *
	 * 성공 케이스:
	 * 1. 올바르 값이
	 * */




	/*
	 * chargeUserPoints()
	 *
	 * 성공 케이스 :
	 * 1. 유저가 정상적으로 포인트 충전
	 * 2.
	 *
	 *
	 *
	 * 로직 :
	 *  유저가 충전?
	 *  일단은 UserTable에 등록을 하고
	 * 		selectById로 조회 없으면 UserPoint 기본값 등록
	 * 			selectById로 등록된 UserPoing가 있으면 amount + UserPoing.amount
	 *
	 *  		그후 insertOrUpdate로 UserPointTable 최신화
	 *
	 *  정상적으로 충정되어 있는지 확인
	 * */

	@Test
	@DisplayName("유저가 정상적으로 포인트 충전")
	void 유저가_정상적으로_포인트_충전() {
		// given
		long userId = 1L;
		long chargeAmount = 1000L;
		TransactionType type = TransactionType.CHARGE;

		// 기존 포인트 확인
		UserPoint initialUserPoint = pointRepository.selectById(userId);
		long initialAmount = initialUserPoint.point();

		// when - 유저 포인트 충전
		UserPoint updatedUserPoint = pointService.chargeUserPoints(userId, chargeAmount, type);
		List<PointHistory> pointHistories = pointRepository.selectAllByUserId(userId);

		// then - 충전된 포인트 확인
		assertThat(pointHistories).isNotEmpty();

		// 가장 최근의 히스토리 항목이 올바르게 기록되었는지 확인
		PointHistory latestHistory = pointHistories.get(pointHistories.size() - 1);
		assertThat(latestHistory.userId()).isEqualTo(userId);
		assertThat(latestHistory.amount()).isEqualTo(chargeAmount);
		assertThat(latestHistory.type()).isEqualTo(TransactionType.CHARGE);
	}

}

