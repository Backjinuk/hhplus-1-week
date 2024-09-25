package io.hhplus.tdd.point.service;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;

@SpringBootTest
class PointServiceSynchroTest {

	@Autowired
	PointService pointService;

	// private long userId;
	//
	// @BeforeEach
	// void setUp() {
	// 	userId = System.currentTimeMillis();  // 유니크한 사용자 ID 생성
	// }V


	/*
	 * 동시성 테스트 시나리오
	 *
	 * 1. 다수의 스레드가 동시에 포인트를 충전할 때 발생할 수 있는 문제를 테스트
	 *    - 여러 스레드가 동시에 같은 유저의 포인트를 충전할 경우, synchronized 키워드가 정상적으로 작동하여
	 *      포인트 값이 올바르게 누적되는지 확인한다.
	 *    - 각 스레드는 일정한 금액(1000)을 충전하고, 모든 스레드가 충전을 완료한 후 최종 포인트가
	 *      스레드 수 * 충전 금액이 되어야 한다.
	 *
	 * 2. 다수의 스레드가 동시에 포인트 충전과 사용을 섞어서 실행할 때 발생할 수 있는 문제를 테스트
	 *    - 50개의 스레드가 포인트를 충전하고, 다른 50개의 스레드는 같은 유저의 포인트를 동시에 사용한다.
	 *    - 각 스레드가 수행하는 충전과 사용 작업이 올바르게 동기화되어 결과적으로 충전 및 사용이 기대한 대로 이루어져야 한다.
	 *    - 초기 충전 금액(5000)을 미리 충전해두고, 절반은 충전, 절반은 사용한다.
	 *    - 충전된 금액과 사용된 금액이 서로 충돌 없이 잘 반영되었는지 검증한다.
	 *
	 * 3. 포인트 충전과 사용을 무작위로 반복할 때 발생할 수 있는 문제를 테스트
	 *    - 스레드가 포인트 충전과 사용을 무작위로 반복해서 실행할 경우, 각 스레드 작업이 충돌 없이 정상적으로 처리되는지 테스트한다.
	 *    - 충전과 사용 작업이 섞여서 발생하는 복잡한 상황에서도 포인트 값이 예상한 대로 누적되고 차감되는지 확인한다.
	 *
	 * 4. 포인트가 충전되고 사용되는 동안 포인트 히스토리가 올바르게 기록되는지 테스트
	 *    - 여러 스레드가 포인트 충전과 사용을 동시에 처리할 때, 포인트 히스토리의 기록이 시간순서대로 정확하게 남는지 확인한다.
	 *    - 포인트가 충전될 때와 사용할 때 모두 히스토리가 기록되어야 하며, 동시 작업 중에도 히스토리가 올바른 순서대로 유지되어야 한다.
	 *
	 */

	@Test
	@DisplayName("여러회원이_한번에_포인트충전")
	void 여러회원이한번에_포인트충전() throws InterruptedException {
		// given
		long userId = 1;
		int threadCount = 1000; // 동시 실행할 스레드 수
		long chargeAmount = 1000L;

		// when
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount); // 고정된 수의 스레드 풀 생성

		for (int i = 0; i < threadCount; i++) {
			executorService.execute(() -> {
				pointService.chargeUserPoints(userId, chargeAmount, TransactionType.CHARGE);
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS); // 모든 스레드가 작업을 끝낼 때까지 기다림

		// then - 포인트가 정확히 누적되었는지 확인
		UserPoint userPoint = pointService.selectUserPoint(userId);
		assertThat(userPoint.point()).isEqualTo(chargeAmount * threadCount);  // threadCount * chargeAmount 만큼 충전되었어야 함
	}

}