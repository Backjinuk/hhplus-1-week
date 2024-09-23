package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
 * 3. 포인트 금액이 0 이하일 때 IllegalArgumentException이 발생한다.
 * 4. 포인트 금액이 너무 클 때(100,000,000 이상) IllegalArgumentException이 발생한다.
 * 5. 트랜잭션 타입이 null일 때 NullPointerException이 발생한다.
 * 6. 업데이트 시간이 0 이하일 때 IllegalArgumentException이 발생한다.
 * 7. 업데이트 시간이 음수일 때 IllegalArgumentException이 발생한다.
 */

class PointHistoryTest {

	// 성공 케이스 테스트 메서드

	@Test
	@DisplayName("정상적인 입력값으로 PointHistory 객체 생성")
	void 정상적인_입력값으로_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(1, 1, 5000, TransactionType.CHARGE, currentMillis);

		// then
		assertEquals(1, pointHistory.id());
		assertEquals(1, pointHistory.userId());
		assertEquals(5000, pointHistory.amount());
		assertEquals(TransactionType.CHARGE, pointHistory.type());
		assertEquals(currentMillis, pointHistory.updateMillis());
	}

	@Test
	@DisplayName("타임스탬프가 현재 시간일 때 PointHistory 객체 생성")
	void 타임스탬프가_현재_시간일_때_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(2, 2, 10000, TransactionType.USE, currentMillis);

		// then
		assertEquals(2, pointHistory.id());
		assertEquals(2, pointHistory.userId());
		assertEquals(10000, pointHistory.amount());
		assertEquals(TransactionType.USE, pointHistory.type());
		assertEquals(currentMillis, pointHistory.updateMillis());
	}

	@Test
	@DisplayName("포인트 금액이 경계값(1)일 때 PointHistory 객체 생성")
	void 포인트_금액이_경계값일_때_PointHistory_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(3, 1, 1, TransactionType.CHARGE, currentMillis);

		// then
		assertEquals(1, pointHistory.amount());
	}

	@Test
	@DisplayName("트랜잭션 타입이 CHARGE일 때 포인트 충전")
	void 트랜잭션_타입이_CHARGE일_때_포인트_충전() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(4, 1, 1000, TransactionType.CHARGE, currentMillis);

		// then
		assertEquals(TransactionType.CHARGE, pointHistory.type());
		assertTrue(pointHistory.amount() > 0);
	}

	@Test
	@DisplayName("트랜잭션 타입이 USE일 때 포인트 사용")
	void 트랜잭션_타입이_USE일_때_포인트_사용() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = new PointHistory(5, 1, 500, TransactionType.USE, currentMillis);

		// then
		assertEquals(TransactionType.USE, pointHistory.type());
		assertTrue(pointHistory.amount() > 0);
	}

	// 실패 케이스 테스트 메서드

	@Test
	@DisplayName("유저 ID가 0 이하일 때 예외 발생")
	void 유저ID가_0이하일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(1, 0, 5000, TransactionType.CHARGE, currentMillis);
		});
		assertEquals("유효하지 않은 유저 ID입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("유저 ID가 음수일 때 예외 발생")
	void 유저ID가_음수일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(2, -1, 5000, TransactionType.CHARGE, currentMillis);
		});
		assertEquals("유효하지 않은 유저 ID입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("포인트 금액이 0 이하일 때 예외 발생")
	void 포인트_금액이_0이하일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(3, 1, 0, TransactionType.USE, currentMillis);
		});
		assertEquals("포인트 금액은 0보다 커야 합니다.", exception.getMessage());
	}

	@Test
	@DisplayName("포인트 금액이 너무 클 때 예외 발생")
	void 포인트_금액이_너무_클_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(4, 1, 100000000, TransactionType.CHARGE, currentMillis);
		});
		assertEquals("충전 금액이 너무 큽니다. 담당자에게 문의 부탁드립니다.", exception.getMessage());
	}

	@Test
	@DisplayName("트랜잭션 타입이 null일 때 예외 발생")
	void 트랜잭션_타입이_null일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		NullPointerException exception = assertThrows(NullPointerException.class, () -> {
			new PointHistory(5, 1, 5000, null, currentMillis);
		});
		assertEquals("트랜잭션 타입이 null 입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("업데이트 시간이 0 이하일 때 예외 발생")
	void 업데이트_시간이_0이하일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(6, 1, 5000, TransactionType.USE, 0);
		});
		assertEquals("유효하지 않은 타임스탬프입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("업데이트 시간이 음수일 때 예외 발생")
	void 업데이트_시간이_음수일_때_예외발생() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new PointHistory(7, 1, 5000, TransactionType.USE, -1000);
		});
		assertEquals("유효하지 않은 타임스탬프입니다.", exception.getMessage());
	}
}
