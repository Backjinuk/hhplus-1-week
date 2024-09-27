package io.hhplus.tdd.point.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PointHistoryTest {

	/**
	 * 성공 케이스:
	 * 1. 정상적인 유저 ID, 포인트 금액, 트랜잭션 타입(USE, CHARGE), 업데이트 시간으로 PointHistory 객체를 생성할 수 있다.
	 * 2. 타임스탬프가 현재 시간일 때 정상적으로 PointHistory 객체를 생성할 수 있다.
	 * 3. 포인트 금액이 경계값(1)일 때 정상적으로 PointHistory 객체를 생성할 수 있다.
	 * 4. 트랜잭션 타입이 CHARGE일 때 포인트 충전.
	 * 5. 트랜잭션 타입이 USE일 때 포인트 사용.
	 *
	 * 실패 케이스:
	 * 1. 유저 ID가 0 이하일 때 IllegalArgumentException이 발생한다.
	 * 2. 유저 ID가 음수일 때 IllegalArgumentException이 발생한다.
	 * 3. 포인트 금액이 너무 클 때(100,000,000 이상) IllegalArgumentException이 발생한다.
	 * 4. 트랜잭션 타입이 null일 때 NullPointerException이 발생한다.
	 * 5. 업데이트 시간이 0 이하일 때 IllegalArgumentException이 발생한다.
	 * 6. 업데이트 시간이 음수일 때 IllegalArgumentException이 발생한다.
	 */

	@Test
	@DisplayName("정상적인 입력값으로 PointHistory 객체 생성")
	void 정상적인_입력값으로_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(1, 1, 5000, TransactionType.CHARGE, currentMillis);

		// then
		assertThat(pointHistory.id()).isEqualTo(1);
		assertThat(pointHistory.userId()).isEqualTo(1);
		assertThat(pointHistory.amount()).isEqualTo(5000);
		assertThat(pointHistory.type()).isEqualTo(TransactionType.CHARGE);
		assertThat(pointHistory.updateMillis()).isEqualTo(currentMillis);
	}

	@Test
	@DisplayName("타임스탬프가 현재 시간일 때 PointHistory 객체 생성")
	void 타임스탬프가_현재_시간일_때_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(2, 2, 10000, TransactionType.USE, currentMillis);

		// then
		assertThat(pointHistory.id()).isEqualTo(2);
		assertThat(pointHistory.userId()).isEqualTo(2);
		assertThat(pointHistory.amount()).isEqualTo(10000);
		assertThat(pointHistory.type()).isEqualTo(TransactionType.USE);
		assertThat(pointHistory.updateMillis()).isEqualTo(currentMillis);
	}

	@Test
	@DisplayName("포인트 금액이 경계값(1)일 때 PointHistory 객체 생성")
	void 포인트_금액이_경계값일_때_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(3, 1, 1, TransactionType.CHARGE, currentMillis);

		// then
		assertThat(pointHistory.amount()).isEqualTo(1);
	}

	@Test
	@DisplayName("트랜잭션 타입이 CHARGE일 때 포인트 충전")
	void 트랜잭션_타입이_CHARGE일_때_포인트_충전() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(4, 1, 1000, TransactionType.CHARGE, currentMillis);

		// then
		assertThat(pointHistory.type()).isEqualTo(TransactionType.CHARGE);
		assertThat(pointHistory.amount()).isGreaterThan(0);
	}

	@Test
	@DisplayName("트랜잭션 타입이 USE일 때 포인트 사용")
	void 트랜잭션_타입이_USE일_때_포인트_사용() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(5, 1, 500, TransactionType.USE, currentMillis);

		// then
		assertThat(pointHistory.type()).isEqualTo(TransactionType.USE);
		assertThat(pointHistory.amount()).isGreaterThan(0);
	}

	// 실패 케이스 테스트 메서드

	@Test
	@DisplayName("유저 ID가 0 이하일 때 예외 발생")
	void 유저ID가_0이하일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(1, 0, 5000, TransactionType.CHARGE, currentMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 유저 ID입니다.");
	}

	@Test
	@DisplayName("유저 ID가 음수일 때 예외 발생")
	void 유저ID가_음수일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(2, -1, 5000, TransactionType.CHARGE, currentMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 유저 ID입니다.");
	}

	@Test
	@DisplayName("포인트 금액이 너무 클 때 예외 발생")
	void 포인트_금액이_너무_클_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(4, 1, 100000000, TransactionType.CHARGE, currentMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("충전 금액이 너무 큽니다. 담당자에게 문의 부탁드립니다.");
	}

	@Test
	@DisplayName("트랜잭션 타입이 null일 때 예외 발생")
	void 트랜잭션_타입이_null일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(5, 1, 5000, null, currentMillis))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("트랜잭션 타입이 null 입니다.");
	}

	@Test
	@DisplayName("업데이트 시간이 0 이하일 때 예외 발생")
	void 업데이트_시간이_0이하일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(6, 1, 5000, TransactionType.USE, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 타임스탬프입니다.");
	}

	@Test
	@DisplayName("업데이트 시간이 음수일 때 예외 발생")
	void 업데이트_시간이_음수일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		assertThatThrownBy(() -> new PointHistory(7, 1, 5000, TransactionType.USE, -1000))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 타임스탬프입니다.");
	}
}
