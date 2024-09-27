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

	/*
	 * 동시성 테스트 시나리오
	 *
	 * 1. 여러 회원이 동시에 포인트를 충전하는 상황을 테스트
	 * 2. 여러 회원이 동시에 포인트를 충전하고 사용하는 상황을 테스트
	 * 3. 랜덤으로 충전과 사용을 반복하는 동시성 테스트
	 * 4. 포인트 부족 시 다중 스레드 재시도 상황을 테스트
	 * 5. 포인트 충전 후 사용 순서 보장을 테스트
	 * 6. 동시 사용 시 포인트 중복 차감 방지를 테스트
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
		int totalThreads = 10; // 총 스레드 수
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



	@Test
	@DisplayName("포인트 부족 시 다중 스레드 재시도 테스트")
	void 포인트_부족_시_다중_스레드_재시도_테스트() throws InterruptedException {
		// given
		long userId = 1;
		long initialCharge = 1000; // 초기 충전 금액
		long useAmount = 500; // 사용할 포인트
		int totalThreads = 10; // 총 스레드 수

		// 사용자에게 초기 포인트를 충전해둠
		pointService.chargeUserPoints(userId, initialCharge, TransactionType.CHARGE);

		ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		// when
		for (int i = 0; i < totalThreads; i++) {
			executorService.execute(() -> {
				try {
					pointService.useUserPoints(userId, useAmount, TransactionType.USE);
					successCount.incrementAndGet();
				} catch (IllegalArgumentException e) {
					// 포인트 부족 예외 발생 시 처리
					System.err.println("포인트 부족 예외 발생");
					failureCount.incrementAndGet();
				}
			});
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);

		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then
		// 포인트 부족으로 인해 실패한 요청이 제대로 카운트되었는지 확인
		assertThat(successCount.get()).isEqualTo(2); // 1000 포인트로 500씩 2번만 사용 가능
		assertThat(failureCount.get()).isEqualTo(8); // 나머지 8번은 포인트 부족으로 실패
	}



	@Test
	@DisplayName("포인트 충전 후 사용 순서 보장 테스트")
	void 포인트_충전_후_사용_순서_보장_테스트() throws InterruptedException {
		// given
		long userId = 1;
		long chargeAmount = 1000;
		long useAmount = 500;
		int totalThreads = 10; // 총 스레드 수

		ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		AtomicInteger chargeCount = new AtomicInteger();
		AtomicInteger useCount = new AtomicInteger();

		// when
		for (int i = 0; i < totalThreads; i++) {
			if (i % 2 == 0) {
				executorService.execute(() -> {
					// 충전 먼저
					pointService.chargeUserPoints(userId, chargeAmount, TransactionType.CHARGE);
					chargeCount.incrementAndGet();
				});
			} else {
				executorService.execute(() -> {
					// 충전 후 사용
					pointService.useUserPoints(userId, useAmount, TransactionType.USE);
					useCount.incrementAndGet();
				});
			}
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);

		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then
		UserPoint userPoint = pointService.selectUserPoint(userId);
		long totalUserPoint = (chargeAmount * chargeCount.get()) - (useAmount * useCount.get());

		// 포인트 검증
		assertThat(userPoint.point()).isEqualTo(totalUserPoint);
		// 충전 횟수와 사용 횟수 검증
		assertThat(chargeCount.get()).isGreaterThanOrEqualTo(useCount.get());
	}



	@Test
	@DisplayName("동시 사용 시 포인트 중복 차감 방지 테스트")
	void 동시_사용_시_포인트_중복_차감_방지_테스트() throws InterruptedException {
		// given
		long userId = 1;
		long initialCharge = 1000; // 초기 충전 금액
		long useAmount = 500; // 사용할 포인트
		int totalThreads = 5; // 총 스레드 수

		// 사용자에게 초기 포인트를 충전해둠
		pointService.chargeUserPoints(userId, initialCharge, TransactionType.CHARGE);

		ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		// when
		for (int i = 0; i < totalThreads; i++) {
			executorService.execute(() -> {
				try {
					pointService.useUserPoints(userId, useAmount, TransactionType.USE);
					successCount.incrementAndGet();
				} catch (IllegalArgumentException e) {
					// 포인트 부족 예외 발생 시 처리
					failureCount.incrementAndGet();
				}
			});
		}

		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);

		if (!terminated) {
			fail("일부 스레드가 지정된 시간 내에 종료되지 않았습니다.");
		}

		// then
		UserPoint userPoint = pointService.selectUserPoint(userId);

		// 1000 포인트에서 500을 2번 사용 가능, 나머지는 실패
		assertThat(successCount.get()).isEqualTo(2);
		assertThat(failureCount.get()).isEqualTo(3);
		assertThat(userPoint.point()).isEqualTo(0); // 사용 후 남은 포인트는 0이어야 함
	}

}