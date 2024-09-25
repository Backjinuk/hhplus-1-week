package io.hhplus.tdd.point.dto;

public record PointHistory(
	long id,
	long userId,
	long amount,
	TransactionType type,
	long updateMillis
) {

	// compact constructor for validation
	public PointHistory {
		if (userId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 유저 ID입니다.");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("포인트 금액은 0보다 작을수 없습니다.");
		}
		if (amount >= 100000000) {
			throw new IllegalArgumentException("충전 금액이 너무 큽니다. 담당자에게 문의 부탁드립니다.");
		}
		if (type == null) {
			throw new NullPointerException("트랜잭션 타입이 null 입니다.");
		}
		if (updateMillis <= 0) {
			throw new IllegalArgumentException("유효하지 않은 타임스탬프입니다.");
		}
	}

}
