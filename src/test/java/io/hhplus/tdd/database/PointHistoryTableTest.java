package io.hhplus.tdd.database;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;

@SpringBootTest
class PointHistoryTableTest {

	@Autowired
	PointHistoryTable pointHistoryTable;

	/*
	 * insert() 메서드 테스트
	 * 성공 케이스 :
	 * 1. insert시 정상 등록 확인
	 * 2. insert시 다수의 데이터 삽입, 커서 증가 확인
	 * 3. insert시 amount가 경계값(1)일때 정상 등록 확인
	 *
	 * 실패 케이스:
	 * 1. insert시 transactionType이 null 일 경우
	 * 2. userId가 음수거나 0일 경우
	 * 3. amount가 음수일 경우
	 * 4. insert시 amount가 너무 클때 예외 발생
	 * 5. insert 시 updateMillis가 0일 때 예외 발생
	 * 6. insert 시 updateMillis가 음수일 때 예외 발생
	 * */

	@Test
	@DisplayName("insert시 정상 등록 확인")
	void insert시_정상_등록_확인() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 1000;
		TransactionType type = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, updateMillis);

		// then
		assertThat(pointHistory.id()).isGreaterThan(0);
		assertThat(pointHistory.userId()).isEqualTo(userId);
		assertThat(pointHistory.amount()).isEqualTo(amount);
		assertThat(pointHistory.type()).isEqualTo(type);
		assertThat(pointHistory.updateMillis()).isEqualTo(updateMillis);
	}

	@Test
	@DisplayName(" insert시 다수의 데이터 삽입, 커서 증가 확인")
	void insert시_다수의_데이터_삽입_커서_증가_확인() {
		// given
		long userId1 = 1, userId2 = 2;
		long amount1 = 1000, amount2 = 2000;
		TransactionType type1 = TransactionType.CHARGE, type2 = TransactionType.USE;
		long updateMillis1 = System.currentTimeMillis(), updateMillis2 = System.currentTimeMillis();

		// when
		PointHistory pointHistory1 = pointHistoryTable.insert(userId1, amount1, type1, updateMillis1);
		PointHistory pointHistory2 = pointHistoryTable.insert(userId2, amount2, type2, updateMillis2);

		// then
		assertThat(pointHistory1.id()).isLessThan(pointHistory2.id());
		assertThat(pointHistory1.userId()).isEqualTo(userId1);
		assertThat(pointHistory2.userId()).isEqualTo(userId2);
		assertThat(pointHistory1.amount()).isEqualTo(amount1);
		assertThat(pointHistory2.amount()).isEqualTo(amount2);
	}

	@Test
	@DisplayName("insert시 amount가 경계값(1)일때 정상 등록 확인")
	void insert시_amount가_경계값일때_정상_등록_확인() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 1; // 경계값
		TransactionType type = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();

		// when
		PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, updateMillis);

		// then
		assertThat(pointHistory.id()).isGreaterThan(0);
		assertThat(pointHistory.amount()).isEqualTo(amount);
	}


	@Test
	@DisplayName("insert 시 TransactionType이 null일 때 예외 발생")
	void insert_null_TransactionType_예외발생() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 1000;
		TransactionType type = null;
		long updateMillis = System.currentTimeMillis();

		// when

		// then
		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("트랜잭션 타입이 null 입니다.");
	}

	@Test
	@DisplayName("insert시 amount가 음수일때 예외 발생")
	void insert시_amount가_음수일때_예외_발생() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = -1;
		TransactionType type = null;
		long updateMillis = System.currentTimeMillis();

		// when

		// then

		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("포인트 금액은 0보다 작을수 없습니다.");
	}

	@Test
	@DisplayName("insert시 userId가 음수일때 예외상황 발생")
	void insert시_userId가_음수일때_예외상황_발생() {
		// given
		long userId = -1;
		long amount = 1000;
		TransactionType type = TransactionType.USE;
		long updateMillis  = System.currentTimeMillis();

		// when

		// then
		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 유저 ID입니다.");
	}

	@Test
	@DisplayName("insert시 amount가 너무 클때 예외 발생")
	void insert시_amount가_너무클때_예외상황() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 100000000;
		TransactionType type = TransactionType.USE;
		long updateMillis  = System.currentTimeMillis();

		// when


		// then
		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("충전 금액이 너무 큽니다. 담당자에게 문의 부탁드립니다.");
	}


	@Test
	@DisplayName("insert 시 updateMillis가 0일 때 예외 발생")
	void insert_updateMillis가_0일_때_예외발생() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 1000;
		TransactionType type = TransactionType.CHARGE;
		long updateMillis = 0; // 0으로 설정

		// when & then
		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 타임스탬프입니다.");
	}

	@Test
	@DisplayName("insert 시 updateMillis가 음수일 때 예외 발생")
	void insert_updateMillis가_음수일_때_예외발생() {
		// given
		long userId = System.currentTimeMillis();  
		long amount = 1000;
		TransactionType type = TransactionType.CHARGE;
		long updateMillis = -1000; // 음수 값 설정

		// when & then
		assertThatThrownBy(() -> pointHistoryTable.insert(userId, amount, type, updateMillis))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("유효하지 않은 타임스탬프입니다.");
	}

	/*
	 * selectAllByUserId() 테스트
	 * 성공 케이스:
	 * 1. selectAllByUserId_존재하는_userId_이력_조회
	 * 2. selectAllByUserId_여러_유저_이력_필터링
	 * 3. selectAllByUserId_데이터_없을_때_빈_리스트_반환
	 *
	 * 실패 케이스:
	 * 1. selectAllByUserId_userId_음수일_때_예외발생
	 */

	@Test
	@DisplayName("selectAllByUserId_존재하는_userId_이력_조회")
	void selectAllByUserId_존재하는_userId_이력_조회() {
		// given
		long userId = System.currentTimeMillis();  
		pointHistoryTable.insert(userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(userId, 500L, TransactionType.USE, System.currentTimeMillis());

		// when
		List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

		// then
		assertThat(pointHistories).hasSize(2);
		assertThat(pointHistories).allMatch(ph -> ph.userId() == userId);
	}


	@Test
	@DisplayName("selectAllByUserId_여러_유저_이력_필터링")
	void selectAllByUserId_여러_유저_이력_필터링() {
		// given
		long userId1 = System.currentTimeMillis(), userId2 = System.currentTimeMillis() + 1;
		pointHistoryTable.insert(userId1, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(userId2, 2000L, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(userId1, 500L, TransactionType.USE, System.currentTimeMillis());

		// when
		List<PointHistory> user1Histories = pointHistoryTable.selectAllByUserId(userId1);
		List<PointHistory> user2Histories = pointHistoryTable.selectAllByUserId(userId2);

		// then
		assertThat(user1Histories).hasSize(2); // userId1에 해당하는 2개의 이력이 있어야 함
		assertThat(user2Histories).hasSize(1); // userId2에 해당하는 1개의 이력이 있어야 함

		assertThat(user1Histories).allMatch(ph -> ph.userId() == userId1);
		assertThat(user2Histories).allMatch(ph -> ph.userId() == userId2);
	}


}