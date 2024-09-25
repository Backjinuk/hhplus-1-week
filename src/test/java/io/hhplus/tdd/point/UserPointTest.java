package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import io.hhplus.tdd.point.dto.UserPoint;

/**
 * 성공 케이스:
 * 1. 정상적인 유저 ID, 포인트, 업데이트 시간으로 UserPoint 객체를 생성할 수 있다.
 * 2. empty 메서드를 사용하여 포인트가 0인 UserPoint 객체를 생성할 수 있다.
 * 3. ID가 1 이상일 때 정상적으로 UserPoint 객체를 생성할 수 있다.
 * 4. 포인트가 0일 때 정상적으로 UserPoint 객체를 생성할 수 있다.
 * 5. 동일한 데이터로 생성된 두 UserPoint 객체가 equals()로 비교 시 동일하다.
 *
 * 실패 케이스:
 * 1. 유저 ID가 0 이하일 때 IllegalArgumentException이 발생한다.
 * 2. 유저 ID가 음수일 때 IllegalArgumentException이 발생한다.
 * 3. 포인트가 0 미만일 때 IllegalArgumentException이 발생한다.
 * 4. 업데이트 시간이 0 이하일 때 IllegalArgumentException이 발생한다.
 * 5. 업데이트 시간이 음수일 때 IllegalArgumentException이 발생한다.
 */

class UserPointTest {

	// 성공 케이스 테스트 메서드

	@Test
	@DisplayName("정상적인 입력값으로 UserPoint 객체 생성")
	void 정상적인_입력값으로_UserPoint_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		UserPoint userPoint = new UserPoint(1, 100, currentMillis);

		// then
		assertThat(userPoint.id()).isEqualTo(1);
		assertThat(userPoint.point()).isEqualTo(100);
		assertThat(userPoint.updateMillis()).isEqualTo(currentMillis);
	}

	@Test
	@DisplayName("빈 UserPoint 객체 생성 (포인트 0)")
	void 빈_UserPoint_객체_생성() {
		// when
		UserPoint userPoint = UserPoint.empty(1);

		// then
		assertThat(userPoint.id()).isEqualTo(1);
		assertThat(userPoint.point()).isEqualTo(0);
		assertThat(userPoint.updateMillis()).isGreaterThan(0);
	}

	@Test
	@DisplayName("ID가 1 이상일 때 UserPoint 객체 생성")
	void ID가_1이상일_때_UserPoint_객체_생성() {
		// when
		UserPoint userPoint = new UserPoint(1, 50, System.currentTimeMillis());

		// then
		assertThat(userPoint.id()).isEqualTo(1);
		assertThat(userPoint.point()).isEqualTo(50);
	}

	@Test
	@DisplayName("포인트가 0일 때 UserPoint 객체 생성")
	void 포인트가_0일_때_UserPoint_객체_생성() {
		// given
		long currentMillis = System.currentTimeMillis();

		// when
		UserPoint userPoint = new UserPoint(1, 0, currentMillis);

		// then
		assertThat(userPoint.id()).isEqualTo(1);
		assertThat(userPoint.point()).isEqualTo(0);
		assertThat(userPoint.updateMillis()).isEqualTo(currentMillis);
	}

	@Test
	@DisplayName("동일한 데이터의 두 UserPoint 객체가 equals()로 동일")
	void 동일한_데이터의_UserPoint_객체가_동일() {
		// given
		long currentMillis = System.currentTimeMillis();
		UserPoint userPoint1 = new UserPoint(1, 100, currentMillis);
		UserPoint userPoint2 = new UserPoint(1, 100, currentMillis);

		// then
		assertThat(userPoint1).isEqualTo(userPoint2);
		assertThat(userPoint1.hashCode()).isEqualTo(userPoint2.hashCode());
	}

	// 실패 케이스 테스트 메서드

	@Test
	@DisplayName("유저 ID가 0 이하일 때 예외 발생")
	void 유저ID가_0이하일_때_예외발생() {
		// when & then
		assertThatThrownBy(() -> new UserPoint(0, 100, System.currentTimeMillis()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}

	@Test
	@DisplayName("유저 ID가 음수일 때 예외 발생")
	void 유저ID가_음수일_때_예외발생() {
		// when & then
		assertThatThrownBy(() -> new UserPoint(-1, 100, System.currentTimeMillis()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}

	@Test
	@DisplayName("포인트가 0 미만일 때 예외 발생")
	void 포인트가_0미만일_때_예외발생() {
		// when & then
		assertThatThrownBy(() -> new UserPoint(1, -10, System.currentTimeMillis()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트는 0보다 작을수 없습니다.");
	}

	@Test
	@DisplayName("업데이트 시간이 0 이하일 때 예외 발생")
	void 업데이트시간이_0이하일_때_예외발생() {
		// when & then
		assertThatThrownBy(() -> new UserPoint(1, 100, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않는 시간입니다.");
	}

	@Test
	@DisplayName("업데이트 시간이 음수일 때 예외 발생")
	void 업데이트시간이_음수일_때_예외발생() {
		// when & then
		assertThatThrownBy(() -> new UserPoint(1, 100, -1000))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않는 시간입니다.");
	}
}
