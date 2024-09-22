package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.database.PointHistoryTable;

@SpringBootTest
class PointServiceTest {

	@Autowired
	PointHistoryTable pointHistoryTable;

	/* *
	 *  UserPoint
	 * */
	@DisplayName("유효하지 않은 UserId로 UserPoint 생성 시 예외 발생 테스트")
	@Test
	void 유효하지_않은_UserId로_UserPoint_생성시_예외발생_테스트() {
		// then
		assertThatThrownBy(() -> new UserPoint(0, 0, System.currentTimeMillis()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}

	@DisplayName("유효하지 않는 point로 UserPoint 생성시 예외 발생")
	@Test
	void 유효하지_않는_point로_UserPoint_생성시_예외_발생() {
		// then
		assertThatThrownBy(() -> new UserPoint(1, -1, System.currentTimeMillis()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트는 0보다 작을수 없습니다.");
	}

	@DisplayName("유효하지 않는 updaetMillis로 UserPoint 생성시 예외 발생")
	@Test
	void 유효하지_않는_updaetMills로_UserPoint_생성시_예외_발생() {
		// then
		assertThatThrownBy(() -> new UserPoint(1, 1000, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않는 시간입니다.");
	}

	/**
	 * pointHistory 단위 테스트 작성
	 * */

	@DisplayName("특정 유저의 포인트 충전하기 정상로직")
	@Test
	void 포인트충전_정상로직_테스트() {
		// given
		UserPoint userPoint = UserPoint.empty(1);
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 1000;

		// when
		PointHistory pointHistory = pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis);

		// then
		assertThat(pointHistory).isNotNull();
		assertThat(pointHistory.amount()).isEqualTo(amount);
		assertThat(pointHistory.amount()).isGreaterThan(0);
	}

	@DisplayName("트랜잭션 타입이 null일 경우 예외 발생")
	@Test
	void 포인트충전_트랜잭션타입이Null_예외발생() {
		// given
		UserPoint userPoint = UserPoint.empty(1);
		TransactionType transactionType = null;  // null 트랜잭션 타입
		long updateMillis = System.currentTimeMillis();
		long amount = 1000;

		// when & then
		assertThatThrownBy(() -> pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("트랜잭션 타입이 null 입니다.");
	}

	@DisplayName("충전 금액이 0 이하일 경우 예외 발생")
	@Test
	void 포인트충전_금액0이하_예외발생() {
		// given
		UserPoint userPoint = UserPoint.empty(1);
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 0;  // 금액이 0인 경우

		// when & then
		assertThatThrownBy(() -> pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트 금액은 0보다 커야 합니다.");
	}

	@DisplayName("충전 금액이 1억이 넘는 경우 예외 발생")
	@Test
	void 포인트충전_금액1억초과_예외발생() {
		// given
		UserPoint userPoint = UserPoint.empty(1);
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 100000001;  // 금액이 1억 초과

		// when & then
		assertThatThrownBy(() -> pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("충전 금액이 너무 큽니다. 담당자에게 문의 부탁드립니다.");
	}

	/**
	 *  List<PointHistory> 단위 테스트 작성
	 * */

	@DisplayName("등록된 회원이 없을 때 포인트 조회 테스트")
	@Test
	void 포인트조회_등록된회원없음_테스트() {
		// given
		UserPoint userPoint = UserPoint.empty(1);

		// when
		List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userPoint.id());

		//then
		assertThatThrownBy(() -> {
			if (pointHistories.isEmpty()) {
				throw new NullPointerException("등록된 회원이 없습니다.");
			}
		}).isInstanceOf(NullPointerException.class)
			.hasMessageContaining("등록된 회원이 없습니다.");

	}

	@DisplayName("등록된 회원이 있을때 조회")
	@Test
	void 포인트조회_등록된회원있음() {
		// given
		UserPoint userPoint = UserPoint.empty(1);
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 10000;  // 금액이 1억 초과

		pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis);

		// when
		List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userPoint.id());

		// then
		assertThat(pointHistories).hasSize(1);
	}

	@DisplayName("등록된 회원이 없을 때 UserPoint와 PointHistory의 정보가 일치하는지 확인")
	@Test
	void 등록된회원이없을때_UserPoint와_PointHistory의_정보가_일치하는지_확인() {
		// given
		long userId = 1;
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, amount, updateMillis);

		// 기존 PointHistory 조회
		List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userPoint.id());

		// 등록된 회원이 없을 때의 상황 확인
		assertThat(pointHistories).isEmpty(); // 회원의 포인트 이력이 없음을 확인

		// when
		// 새로운 포인트 기록 삽입
		PointHistory pointHistory = pointHistoryTable.insert(userPoint.id(), amount, transactionType, updateMillis);

		// then
		assertThat(pointHistory).isNotNull();  // 포인트 기록이 생성되었는지 확인
		assertThat(pointHistory.amount()).isEqualTo(amount);  // 금액이 예상과 일치하는지 확인
		assertThat(pointHistory.amount()).isGreaterThan(0);  // 금액이 0보다 큰지 확인
	}

	@DisplayName("등록된 회원이 있을때 List에 있는 amount를 합산 ")
	@Test
	void 등록된_회원이_있을때_List에_있는_amout를_합산후_UserPoint에_넣기() {
		// given
		long userId = 1;
		TransactionType transactionType = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		long amount = 1000;

		pointHistoryTable.insert(userId, amount, transactionType, updateMillis);

		userId = 1;
		transactionType = TransactionType.CHARGE;
		updateMillis = System.currentTimeMillis();
		amount = 3000;

		pointHistoryTable.insert(userId, amount, transactionType, updateMillis);

		long point = pointHistoryTable.selectAllByUserId(userId)
			.stream()
			.mapToLong(PointHistory::amount)
			.sum();

		// 기존 PointHistory 조회

		// when
		UserPoint userPoint = new UserPoint(userId, point, updateMillis);

		// then
		assertThat(userPoint.point()).isEqualTo(4000);
	}
}

