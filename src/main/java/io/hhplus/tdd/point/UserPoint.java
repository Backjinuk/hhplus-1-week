package io.hhplus.tdd.point;

public record UserPoint(
	long id,
	long point,
	long updateMillis
) {

	public static UserPoint empty(long id) {
		return new UserPoint(id, 0, System.currentTimeMillis());
	}

	public UserPoint {
		if (id <= 0) {
			throw new IllegalArgumentException("유효 하지 않은 UserId 입니다.");
		}
		if (point < 0) {
			throw new IllegalArgumentException("포인트는 0보다 작을수 없습니다.");
		}
		if (updateMillis <= 0) {
			throw new IllegalArgumentException("유효하지 않는 시간입니다.");
		}
	}
}
