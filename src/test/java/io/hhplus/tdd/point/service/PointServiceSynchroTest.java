package io.hhplus.tdd.point.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.point.dto.PointHistory;
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
		long userId1 = 1;
		long userId2 = 2;
		long userId3 = 3;
		int threadCountPerUser = 20; // 각 유저에 대해 실행할 스레드 수
		long chargeAmount = 1000;

		// when
		ExecutorService executorService = Executors.newFixedThreadPool(threadCountPerUser * 3); // 스레드 수를 총 유저 수에 맞춰 늘림

		// 각 회원에 대해 별도의 스레드에서 충전 작업 수행
		for (int i = 0; i < threadCountPerUser; i++) {
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1) 시작");
				pointService.chargeUserPoints(userId1, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1) 종료");
			});
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2) 시작");
				pointService.chargeUserPoints(userId2, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2) 종료");
			});
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3) 시작");
				pointService.chargeUserPoints(userId3, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3) 종료");
			});
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS); // 모든 스레드가 작업을 끝낼 때까지 기다림

		// awaitTermination의 결과를 체크
		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then - 각 회원의 포인트가 정확히 누적되었는지 확인
		UserPoint userPoint1 = pointService.selectUserPoint(userId1);
		UserPoint userPoint2 = pointService.selectUserPoint(userId2);
		UserPoint userPoint3 = pointService.selectUserPoint(userId3);

		assertThat(userPoint1.point()).isEqualTo(chargeAmount * threadCountPerUser);
		assertThat(userPoint2.point()).isEqualTo(chargeAmount * threadCountPerUser);
		assertThat(userPoint3.point()).isEqualTo(chargeAmount * threadCountPerUser);

	}

	@Test
	@DisplayName("여러회원이_동시에_포인트_충전과_사용_번갈아가면서_테스트")
	void 여러회원이_동시에_포인트_충전과_사용_번갈아가면서_테스트() throws InterruptedException {
		// given
		long userId1 = 1;
		long userId2 = 2;
		long userId3 = 3;
		int threadCountPerUser = 20; // 각 유저에 대해 실행할 스레드 수
		long chargeAmount = 1000;
		long useAmount = 500;

		// when
		ExecutorService executorService = Executors.newFixedThreadPool(threadCountPerUser * 3); // 스레드 수를 총 유저 수에 맞춰 늘림

		// 각 회원에 대해 충전과 사용을 번갈아 수행
		for (int i = 0; i < threadCountPerUser; i++) {
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1 충전) 시작");
				pointService.chargeUserPoints(userId1, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1 충전) 종료");
			});
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1 사용) 시작");
				pointService.useUserPoints(userId1, useAmount, TransactionType.USE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User1 사용) 종료");
			});

			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2 충전) 시작");
				pointService.chargeUserPoints(userId2, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2 충전) 종료");
			});
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2 사용) 시작");
				pointService.useUserPoints(userId2, useAmount, TransactionType.USE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User2 사용) 종료");
			});

			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3 충전) 시작");
				pointService.chargeUserPoints(userId3, chargeAmount, TransactionType.CHARGE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3 충전) 종료");
			});
			executorService.execute(() -> {
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3 사용) 시작");
				pointService.useUserPoints(userId3, useAmount, TransactionType.USE);
				System.out.println("Thread " + Thread.currentThread().getId() + " (User3 사용) 종료");
			});
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS); // 모든 스레드가 작업을 끝낼 때까지 기다림

		// awaitTermination의 결과를 체크
		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then - 각 회원의 포인트가 정확히 계산되었는지 확인
		UserPoint userPoint1 = pointService.selectUserPoint(userId1);
		UserPoint userPoint2 = pointService.selectUserPoint(userId2);
		UserPoint userPoint3 = pointService.selectUserPoint(userId3);

		List<PointHistory> pointHistories1 = pointService.selectUserPointHistory(userId1);
		List<PointHistory> pointHistories2 = pointService.selectUserPointHistory(userId2);
		List<PointHistory> pointHistories3 = pointService.selectUserPointHistory(userId3);

		// 최종 포인트 확인: chargeAmount * threadCountPerUser - useAmount * threadCountPerUser
		assertThat(userPoint1.point()).isEqualTo((chargeAmount - useAmount) * threadCountPerUser);
		assertThat(userPoint2.point()).isEqualTo((chargeAmount - useAmount) * threadCountPerUser);
		assertThat(userPoint3.point()).isEqualTo((chargeAmount - useAmount) * threadCountPerUser);

		assertThat(pointHistories1).hasSize(40);
		assertThat(pointHistories2).hasSize(40);
		assertThat(pointHistories3).hasSize(40);

	}

	@Test
	@DisplayName("랜덤 충전 및 사용 동시성 테스트")
	void 랜덤_충전_및_사용_동시성_테스트() throws InterruptedException {
		// given
		long userId1 = 1;
		long chargeAmount = 1000;
		long useAmount = 500;
		int totalThreads = 2; // 총 스레드 수
		AtomicInteger chargeCount = new AtomicInteger();
		AtomicInteger useCount = new AtomicInteger();

		ExecutorService executorService = Executors.newFixedThreadPool(totalThreads * 2);

		// when
		for (int i = 0; i < totalThreads; i++) {
			int random = (int)(Math.random() * 2); // 0 또는 1 무작위로 생성
			if (random == 0) {
				executorService.execute(() -> {
					System.out.println("Thread " + Thread.currentThread().getId() + " (충전) 시작");
					pointService.chargeUserPoints(userId1, chargeAmount, TransactionType.CHARGE);
					chargeCount.incrementAndGet();
					System.out.println("Thread " + Thread.currentThread().getId() + " (충전) 종료");
				});
			} else {
				executorService.execute(() -> {
					System.out.println("Thread " + Thread.currentThread().getId() + " (사용) 시작");
					pointService.useUserPoints(userId1, useAmount, TransactionType.USE);
					useCount.incrementAndGet();
					System.out.println("Thread " + Thread.currentThread().getId() + " (사용) 종료");
				});
			}
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(240, TimeUnit.SECONDS);

		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then
		UserPoint userPoint = pointService.selectUserPoint(userId1);
		List<PointHistory> pointHistories = pointService.selectUserPointHistory(userId1);

		long totalUserPoint = (chargeAmount * chargeCount.get()) - (useAmount * useCount.get());

		assertThat(userPoint.point()).isEqualTo(totalUserPoint);
		assertThat(pointHistories).hasSize(chargeCount.get() + useCount.get());
	}
}