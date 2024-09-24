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
	 * chargeUserPoints()
	 *
	 * 성공 케이스 :
	 * 1. 유저가 정상적으로 포인트 충전
	 * 2. 유저가_중복해서_포인트_충전
	 * 3. 서로다른_유저가_중복해서_포인트_충전
	 *
	 * 실패 케이스:
	 * 1. 잘못된 userId가 들어왔을때
	 * 2. 충전 금액이 음수인 경우
	 */

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
		assertThat(updatedUserPoint.point()).isEqualTo(initialAmount + chargeAmount);
		assertThat(pointHistories).isNotEmpty();

		// 가장 최근의 히스토리 항목이 올바르게 기록되었는지 확인
		PointHistory latestHistory = pointHistories.get(pointHistories.size() - 1);
		assertThat(latestHistory.userId()).isEqualTo(userId);
		assertThat(latestHistory.amount()).isEqualTo(chargeAmount);
		assertThat(latestHistory.type()).isEqualTo(TransactionType.CHARGE);
	}

	@Test
	@DisplayName("유저가 중복해서 포인트 충전")
	void 유저가_중복해서_포인트_충전() {
		// given
		long userId = 1L;
		long firstChargeAmount = 1000L;
		long secondChargeAmount = 2000L;
		TransactionType type = TransactionType.CHARGE;

		// when - 첫 번째 충전
		UserPoint firstUpdate = pointService.chargeUserPoints(userId, firstChargeAmount, type);
		// 두 번째 충전
		UserPoint secondUpdate = pointService.chargeUserPoints(userId, secondChargeAmount, type);

		// then - 두 번째 충전 후의 포인트 확인
		assertThat(firstUpdate.point()).isEqualTo(firstChargeAmount);
		assertThat(secondUpdate.point()).isEqualTo(firstChargeAmount + secondChargeAmount);

		// 히스토리 기록 확인
		List<PointHistory> pointHistories = pointRepository.selectAllByUserId(userId);
		assertThat(pointHistories).hasSize(2); // 충전 2번의 기록 확인
	}

	@Test
	@DisplayName("서로 다른 유저가 중복해서 포인트 충전")
	void 서로다른_유저가_중복해서_포인트_충전() {
		// given
		long userId1 = 1L, userId2 = 2L;
		long user1Charge1 = 1000L, user1Charge2 = 2000L;
		long user2Charge1 = 500L, user2Charge2 = 1500L;
		TransactionType type = TransactionType.CHARGE;

		// when - 첫 번째 유저 충전
		UserPoint user1FirstUpdate = pointService.chargeUserPoints(userId1, user1Charge1, type);
		UserPoint user1SecondUpdate = pointService.chargeUserPoints(userId1, user1Charge2, type);

		// 두 번째 유저 충전
		UserPoint user2FirstUpdate = pointService.chargeUserPoints(userId2, user2Charge1, type);
		UserPoint user2SecondUpdate = pointService.chargeUserPoints(userId2, user2Charge2, type);

		// then - 각 유저의 포인트 확인
		assertThat(user1FirstUpdate.point()).isEqualTo(user1Charge1);
		assertThat(user1SecondUpdate.point()).isEqualTo(user1Charge1 + user1Charge2);

		assertThat(user2FirstUpdate.point()).isEqualTo(user2Charge1);
		assertThat(user2SecondUpdate.point()).isEqualTo(user2Charge1 + user2Charge2);
	}

	@Test
	@DisplayName("잘못된 userId가 들어왔을 때 예외 발생")
	void 잘못된_userId가_들어왔을때() {
		// given
		long invalidUserId = 0L;
		long chargeAmount = 1000L;
		TransactionType type = TransactionType.CHARGE;

		// when & then
		assertThatThrownBy(() -> pointService.chargeUserPoints(invalidUserId, chargeAmount, type))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}

	@Test
	@DisplayName("충전 금액이 음수인 경우 예외 발생")
	void 충전_금액이_음수인_경우() {
		// given
		long userId = 1L;
		long invalidChargeAmount = -1L;
		TransactionType type = TransactionType.CHARGE;

		// when & then
		assertThatThrownBy(() -> pointService.chargeUserPoints(userId, invalidChargeAmount, type))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트는 0보다 작을수 없습니다.");
	}







	/*
	 * getUserPoints()
	 *
	 * 성공 케이스:
	 * 1. 유저의 정보가 있을때 유저의 포인트 조회
	 * 2. 유저의 정보가 없을때 유저의 포인트 조회
	 *
	 * 실패 케이스 :
	 * 1. 유저의 아이디가 올바르지 않는 경우
	 * */

	@Test
	@DisplayName("유저의 정보가 있을때 유저의 포인트 조회")
	void 유저의정보가있을때_유저의포인트조회() {
		// given
		long userId = 1;
		long amount = 1000;
		TransactionType type = TransactionType.CHARGE;
		pointService.chargeUserPoints(userId, amount, type);

		// when
		UserPoint userPoint = pointService.selectUserPoint(userId);

		// then
		assertThat(userPoint.id()).isEqualTo(userId);
		assertThat(userPoint.point()).isEqualTo(1000);
	}

	@Test
	@DisplayName("유저의정보가 없을때 유저의 포인트 조회")
	void 유저의정보가없을때_유저의포인트조회() {
		// given
		long userId = 1;

		// when
		UserPoint userPoint = pointService.selectUserPoint(userId);

		// then
		assertThat(userPoint.id()).isEqualTo(1);
		assertThat(userPoint.point()).isEqualTo(0);

	}

	@Test
	@DisplayName("유저의아이디가 올바르지 않는경우")
	void 유저의아이디가_올바르지_않는경우() {
		// given
		long userId = 0;

		// when

		// then
		assertThatThrownBy(() -> pointService.selectUserPoint(userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}



	/*
	 * useUserPoint
	 *
	 * 성공 케이스 :
	 * 1. 모든 값이 정상일 때 포인트 사용
	 *
	 * 실패 케이스:
	 * 1. 사용할 포인트가 현재 가지고 있는 포인트보다 많을 때
	 * 2. 사용할 포인트가 음수일 때
	 */

	@Test
	@DisplayName("모든 값이 정상일 때 포인트 사용")
	void 모든값이정상일때_포인트사용() {
		// given
		long userId = 1;
		long chargeAmount = 1000;
		TransactionType chargeType = TransactionType.CHARGE;
		TransactionType useType = TransactionType.USE;
		pointService.chargeUserPoints(userId, chargeAmount, chargeType);  // 포인트 충전

		// when
		UserPoint userPoint = pointService.useUserPoints(userId, 700, useType);  // 포인트 사용

		// then
		assertThat(userPoint.point()).isEqualTo(300L);  // 포인트가 700만큼 차감된 결과

		// 히스토리 검증
		List<PointHistory> pointHistories = pointRepository.selectAllByUserId(userId);
		assertThat(pointHistories).isNotEmpty();
		PointHistory latestHistory = pointHistories.get(pointHistories.size() - 1);
		assertThat(latestHistory.amount()).isEqualTo(700);  // 사용된 포인트 확인
		assertThat(latestHistory.type()).isEqualTo(TransactionType.USE);
	}

	@Test
	@DisplayName("사용할 포인트가 현재 가지고 있는 포인트보다 많을 때")
	void 사용할_포인트가_현재_가지고_있는_포인트보다_많을때() {
		// given
		long userId = 1;
		long chargeAmount = 1000;
		TransactionType chargeType = TransactionType.CHARGE;
		pointService.chargeUserPoints(userId, chargeAmount, chargeType);  // 포인트 충전

		// when & then
		assertThatThrownBy(() -> pointService.useUserPoints(userId, 1700, TransactionType.USE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("사용할 포인트가 없습니다.");
	}

	@Test
	@DisplayName("사용할 포인트가 음수일 때 예외 발생")
	void 사용할_포인트가_음수일때_예외발생() {
		// given
		long userId = 1L;
		long chargeAmount = 1000L;
		TransactionType chargeType = TransactionType.CHARGE;
		pointService.chargeUserPoints(userId, chargeAmount, chargeType);  // 포인트 충전

		// when & then
		assertThatThrownBy(() -> pointService.useUserPoints(userId, -500, TransactionType.USE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트는 0보다 작을수 없습니다.");
	}





	/*
	 *
	 * getUserPoint()
	 *
	 * 성공 케이스 :
	 * 1. 데이터에 존재하는 유저의 정보를 가지고옴
	 * 2. 데이터에 존재하지 않는 유저의 정보를 가지고옴
	 *
	 * 실패 케이스 :
	 * 1. 잘못된 유저의 정보를 가지고옴
	 *
	 * */

	@Test
	@DisplayName("존재하는유저를 가지고옴")
	void 존재하는유저를_가지고옴() {
		// given
		long userId = 1L;
		long chargeAmount = 1000L;
		TransactionType chargeType = TransactionType.CHARGE;
		pointService.chargeUserPoints(userId, chargeAmount, chargeType);  // 포인트 충전

		// when
		UserPoint userPoint = pointService.selectUserPoint(userId);

		// then
		assertThat(userPoint.id()).isEqualTo(userId);
		assertThat(userPoint.point()).isEqualTo(chargeAmount);
	}

	@Test
	@DisplayName("존재하지않는유저를 가지고옴")
	void 존재하지않은유저를_가지고옴() {
		// given
		long userId = 1;

		// when
		UserPoint userPoint = pointService.selectUserPoint(userId);

		// then
		assertThat(userPoint.id()).isEqualTo(userId);
		assertThat(userPoint.point()).isEqualTo(0);
	}

	@Test
	@DisplayName("잘못된유저의 정보를가지고옴")
	void 잘못된유저의_정보를가지고옴() {
		// given
		long userId = 0;

		// when

		// then
		assertThatThrownBy(() -> pointService.selectUserPoint(userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");

	}

}

