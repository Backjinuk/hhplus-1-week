package io.hhplus.tdd.database;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.point.dto.UserPoint;

@SpringBootTest
class UserPointTableTest {

	@Autowired
	UserPointTable userPointTable;

	/**
	 * insertOrUpdate()
	 * 성공 케이스:
	 * 1. insertOrUpdate_올바른_값으로_등록
	 * 2. insertOrUpdate_업데이트_값_반영_테스트
	 * 3. insertOrUpdate_시간_갱신_테스트
	 * 4. insertOrUpdate_amount가_경계값일때_정상등록
	 *
	 * 실패 케이스:
	 * 1. inserOrUpdate시_id가_0인_경우
	 * 2. insertOrUpdate시_amout가_음수인경우
	 * 3. insertOrUpdate_null_ID_예외발생
	 *
	 * */

	@Test
	@DisplayName("insertOrUpdate 올바른 값으로 등록")
	void insertOrUpdate_올바른_값으로_등록() {
		// given
		long id = System.currentTimeMillis();  
		long amount = 1000;

		// when
		UserPoint userPoint = userPointTable.insertOrUpdate(id, amount);

		// then
		assertThat(userPoint.id()).isEqualTo(id);
		assertThat(userPoint.point()).isEqualTo(amount);
	}



	@Test
	@DisplayName("insertOrUpdate시 id가 0인 경우")
	void inserOrUpdate시_id가_0인_경우() {
		// given
		long id = 0;
		long amount = 1000;

		// when

		// then
		assertThatThrownBy(() -> userPointTable.insertOrUpdate(id, amount))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효 하지 않은 UserId 입니다.");
	}


	@Test
	@DisplayName("insertOrUpdate시 amount가 경계값(1)일때 정상 등록 확인")
	void insertOrUpdate_경계값_등록_확인() {
		// given
		long id = System.currentTimeMillis();  
		long amount = 1; // 경계값

		// when
		UserPoint userPoint = userPointTable.insertOrUpdate(id, amount);

		// then
		assertThat(userPoint.id()).isEqualTo(id);
		assertThat(userPoint.point()).isEqualTo(amount);
	}


	@Test
	@DisplayName("insertOrUpdate시 amount가 음수인경우")
	void insertOrUpdate시_amout가_음수인경우() {
		// given
		long id = System.currentTimeMillis();  
		long amount = -1;

		// when

		// then
		assertThatThrownBy(() -> userPointTable.insertOrUpdate(id, amount))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트는 0보다 작을수 없습니다.");
	}

	@Test
	@DisplayName("insertOrUpdate시 update가 정상적으로 되는지 확인")
	void insertOrUpdate시_update가_정상적으로_되는지_확인() {
		// given
		long id = System.currentTimeMillis();  
		long initAmount = 1000;
		long updateAmount = 3000;

		// when
		userPointTable.insertOrUpdate(id, initAmount);
		UserPoint userPoint = userPointTable.insertOrUpdate(id, updateAmount);

		// then
		assertThat(userPoint.id()).isEqualTo(id);
		assertThat(userPoint.point()).isEqualTo(updateAmount);
	}

	@Test
	@DisplayName("insertOrUpdate 호출시 updateMillis가 정상적으로 최신화 되는지 확인")
	void test() throws InterruptedException {
		// given
		long id = System.currentTimeMillis();  
		long amount = 1000;

		// when
		long initialMillis = userPointTable.insertOrUpdate(id, amount).updateMillis();

		//약간 지연
		try {
			Thread.sleep(10);
		} catch (InterruptedException ignored) {
		}

		long initialMillis2 = userPointTable.insertOrUpdate(id, amount).updateMillis();

		// then
		assertThat(initialMillis2).isGreaterThan(initialMillis);
	}

	@Test
	@DisplayName("insertOrUpdate에 null ID가 전달되면 예외 발생")
	void insertOrUpdate_null_ID_예외발생() {
		// given
		Long id = null;
		long amount = 1000;

		// when & then
		assertThatThrownBy(() -> userPointTable.insertOrUpdate(id, amount))
			.isInstanceOf(NullPointerException.class);
	}



	/* *
	 * selectById() 메서드 테스트
	 * 성공 케이스:
	 * 1. selectById는 존재하는 ID에 대해 올바른 UserPoint 객체를 반환한다.
	 * 2. selectById는 존재하지 않는 ID에 대해 기본 UserPoint 객체를 반환한다.
	 * 3. selectById는 동일한 ID로 여러 번 호출해도 동일한 UserPoint 객체를 반환한다.
	 * 4. selectById는 여러 개의 ID에 대해 각각 올바른 UserPoint 객체를 반환한다.
	 * 5. selectById는 insertOrUpdate로 업데이트된 값을 반환한다.
	 *
	 */

	@Test
	@DisplayName("selectById 존재하는 ID를 조회")
	void selectById_존재하는_Id() {
		// given
		long id = System.currentTimeMillis();  
		long amount = 1000;
		userPointTable.insertOrUpdate(id, amount);

		// when
		UserPoint userPoint = userPointTable.selectById(id);

		// then
		assertThat(userPoint.id()).isEqualTo(id);
		assertThat(userPoint.point()).isEqualTo(amount);
	}

	@Test
	@DisplayName("selectById 존재하지 않는 Id 조회시 userPoint.empty 호출")
	void selectById_존재하지_않는_Id_조회시_userPoint_empty_호출() {
		// given
		long id = System.currentTimeMillis();  

		// when
		UserPoint userPoint = userPointTable.selectById(id);

		// then
		assertThat(userPoint.id()).isEqualTo(id);
		assertThat(userPoint.point()).isEqualTo(0); // 존재하지 않는 ID에 대해 포인트는 0
		assertThat(userPoint.updateMillis()).isGreaterThan(0); // updateMillis는 현재 시간
	}

	@Test
	@DisplayName("동일한Id 로 여러번 호출해도 동일한 UserPoint 객체를 반환한다.")
	void 동일한id로_여러번_호출해도_동일한_UserPoint_객체를_반환한다() {

		// given
		long id = System.currentTimeMillis();  
		long amount = 1000;
		userPointTable.insertOrUpdate(id, amount);

		// when
		UserPoint userPoint1 = userPointTable.selectById(id);
		UserPoint userPoint2 = userPointTable.selectById(id);

		// then
		assertThat(userPoint1).isEqualTo(userPoint2);

	}

	@Test
	@DisplayName("selectById 여러 개의 ID에 대해 각각 올바른 UserPoint 객체 반환")
	void selectById_여러_ID에_대한_테스트() {
		// given
		long id1 = 10, id2 = 20;
		long amount1 = 1000, amount2 = 5000;
		userPointTable.insertOrUpdate(id1, amount1);
		userPointTable.insertOrUpdate(id2, amount2);

		// when
		UserPoint result1 = userPointTable.selectById(id1);
		UserPoint result2 = userPointTable.selectById(id2);

		// then
		assertThat(result1.id()).isEqualTo(id1);
		assertThat(result1.point()).isEqualTo(amount1);

		assertThat(result2.id()).isEqualTo(id2);
		assertThat(result2.point()).isEqualTo(amount2);
	}

	@Test
	@DisplayName("insertOrUpdate로 업데이트된 값을 반환한다.")
	void insertOrUpdate로_업데이트된_값_반환() {
		// given
		long id = 20;
		long initialAmount = 2000;
		long updatedAmount = 3000;
		userPointTable.insertOrUpdate(id, initialAmount);

		userPointTable.insertOrUpdate(id, updatedAmount);

		// when
		UserPoint userPoint = userPointTable.selectById(id);

		// then
		assertThat(userPoint.point()).isEqualTo(updatedAmount);
	}

}