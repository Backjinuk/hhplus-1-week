# 동시성 테스트 및 PointService

## 개요

`PointService`는 여러 사용자가 동시에 포인트를 충전하거나 사용할 때 발생할 수 있는 **동시성 문제**를 해결하기 위해 설계되었습니다. 이 서비스는 다중 스레드 환경에서 안전하게 작동할 수 있도록 `ReentrantLock`을 활용하여 **유저별로 락을 관리**하며, 포인트 사용 시 발생할 수 있는 **포인트 부족** 문제를 처리하기 위해 **큐**를 사용하여 실패한 요청을 관리하고 재시도합니다.

이 문서에서는 동시성 문제 해결을 위한 코드 설계와 테스트 방법에 대해 설명합니다.

---

## 동시성 제어에 대한 개념

동시성 문제를 해결하기 위해 여러 가지 기법이 사용됩니다. `PointService`에서는 `ReentrantLock`을 사용하지만, 다양한 동시성 제어 방법이 존재하며 각 방법의 장단점이 있습니다.

1. **`synchronized`**
   - `synchronized` 키워드를 사용하면 한 번에 하나의 스레드만 공유 자원에 접근할 수 있도록 제어합니다.
   - 단점:
      1. 락을 걸고 해지하는 과정에서 성능 저하가 발생할 수 있습니다.
      2. 여러 스레드가 동일한 자원을 사용하려고 할 때 **락 경합**이 발생하며, 이는 **병목 현상**을 유발할 수 있습니다.

2. **`volatile`**
   - `volatile` 변수는 여러 스레드에서 즉시 값을 반영하도록 보장합니다.
   - 단점:
      1. **원자적 연산**을 보장하지 않으며, 단순한 읽기 및 쓰기에는 적합하지만 복잡한 작업에서는 적합하지 않습니다.
      2. 원자적 연산이란 연산이 중간에 끼어들거나 방해받지 않고 완전히 이루어지는 것을 의미합니다. `volatile`는 이러한 원자성을 보장하지 않습니다.

3. **ReentrantLock**
   - `ReentrantLock`은 `synchronized`보다 더 유연하고 강력한 락을 제공하지만, 올바르게 관리해야만 효과적입니다.
   - 특징:
      1. **명시적으로 락을 획득하고 해지**해야 하며, 자원을 제대로 해제하지 않으면 **데드락**이 발생할 수 있습니다.
      2. 경합이 많아지면 성능이 저하될 수 있습니다. 경합은 여러 스레드가 **동시에 동일한 자원에 접근하려고 시도**할 때 발생합니다.
      3. `ReentrantLock`은 **공정성(fairness)**을 기본적으로 보장하지 않으며, 공정성을 보장하기 위해 추가적인 자원 관리가 필요합니다. 이는 성능 저하로 이어질 수 있습니다.

4. **`Executor` 프레임워크**
   - `Executor` 프레임워크는 스레드를 효율적으로 관리하는 데 유용하며, 스레드풀을 사용하여 자원을 효율적으로 관리합니다.
   - 주의사항:
      1. 스레드 풀의 크기를 적절히 설정해야 합니다. 너무 많은 스레드를 생성하면 자원 부족 현상이 발생할 수 있습니다.
      2. 스레드 풀이 적절히 종료되지 않으면 **스레드 누수**가 발생할 수 있습니다.
      3. 에러 처리가 복잡해질 수 있으며, 예외 처리와 종료 시 스레드의 상태를 관리해야 합니다.

---

## 주요 기능 및 동작 원리

1. **ReentrantLock을 이용한 유저별 동시성 제어**
   - 여러 스레드가 동시에 동일한 사용자의 포인트를 변경하려고 할 때 발생할 수 있는 문제를 해결하기 위해, 유저별로 `ReentrantLock`을 사용합니다.
   - `ConcurrentHashMap<Long, Lock>`을 통해 각 사용자의 ID에 해당하는 락을 관리하여, 충돌 없이 포인트 충전 및 사용을 안전하게 처리할 수 있습니다.
```java
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();


	// 유저별 Lock을 관리하는 ConcurrentHashMap
	private Lock getUserLock(long userId) {
		return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
	}

	// 포인트 충전
	public UserPoint chargeUserPoints(long userId, long chargeAmount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			// 포인트 조회 및 업데이트 부분만 락으로 보호
			UserPoint userPoint = selectUserPoint(userId);
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() + chargeAmount);
			recordPointHistory(updatedUserPoint, type);

			return updatedUserPoint;
		} finally {
			lock.unlock(); // 락 해제
		}
	}

	// 포인트 사용
	public UserPoint useUserPoints(long userId, long amount, TransactionType type) {
		Lock lock = getUserLock(userId);
		lock.lock();

		try {
			// 포인트 조회
			UserPoint userPoint = selectUserPoint(userId);

			// 포인트가 부족한 경우 예외 발생 및 큐에 요청 저장
			if (userPoint.point() < amount) {
				failedRequests.offer(new PointHistory(0, userId, amount, type, 1));
				// System.out.println("포인트 부족으로 큐에 저장된 요청: " + failedRequests); // 큐에 저장될 때 콘솔 출력
				throw new IllegalArgumentException("사용할 포인트가 없습니다.");
			}

			// 포인트 사용
			UserPoint updatedUserPoint = updateUserPoints(userPoint.id(), userPoint.point() - amount);
			recordPointHistory(UserPoint.changePoint(updatedUserPoint, amount), type);

			return updatedUserPoint;
		} finally {
			lock.unlock();

			// 큐에 있는 실패한 요청을 재시도
			retryFailedRequests(userId);
		}
	}


```
2. **포인트 부족 시 실패한 요청을 관리하는 큐**
   - 사용자가 사용할 포인트가 부족한 경우, 해당 요청은 **실패 큐**(`failedRequests`)에 저장됩니다.
   - 포인트 충전 후에는 큐에 저장된 실패한 요청을 재시도하여, 포인트가 충분해지면 자동으로 처리됩니다.
   - **ConcurrentLinkedQueue**를 사용하여 다중 스레드 환경에서 안전하게 실패한 요청을 관리합니다.
```java
private final Queue<PointHistory> failedRequests = new ConcurrentLinkedQueue<PointHistory>();
```
3. **실패한 요청 재시도 로직**
   - 포인트가 부족하여 사용에 실패한 요청은 나중에 재시도됩니다. 실패한 요청은 사용자의 포인트가 충분할 때 다시 처리됩니다.
   - **재시도 로직**은 포인트가 충전될 때마다 실행되며, 해당 사용자의 요청만 처리하고, 여전히 포인트가 부족한 경우 다시 큐에 저장됩니다.
```java

	// 큐에 저장된 실패한 요청을 재시도
	private void retryFailedRequests(long userId) {
		Queue<PointHistory> localQueue = new ConcurrentLinkedQueue<>();

		// 유저 ID와 일치하는 실패 요청만 처리
		for (PointHistory failedRequest : failedRequests) {
			if (failedRequest.userId() == userId) {
				try {

					long point = pointRepository.selectUserPoint(userId).point();

					if(point > failedRequest.amount()){

						// 포인트 사용 재시도
						useUserPoints(failedRequest.userId(), failedRequest.amount(), failedRequest.type());
					}
				} catch (IllegalArgumentException e) {
					// 포인트 부족으로 재시도 실패
					System.err.println("포인트 부족: UserId = " + userId);
					localQueue.offer(failedRequest); // 실패한 요청은 다시 로컬 큐에 넣음
				}
			} else {
				localQueue.offer(failedRequest); // 다른 유저의 요청은 로컬 큐에 유지
			}
		}

		failedRequests.clear(); // 기존 큐 비우기
		failedRequests.addAll(localQueue); // 처리되지 않은 요청 다시 삽입
	}
```
---

## 동시성 문제 해결을 위한 설계

1. **ReentrantLock을 통한 동시성 제어**
   - `chargeUserPoints()`와 `useUserPoints()` 메서드는 각 유저별로 락을 걸어 포인트 조회 및 업데이트 작업을 보호합니다.
   - 락을 사용하여 다수의 스레드가 동시에 접근해도 **정확한 포인트 계산**이 보장됩니다.

2. **실패 큐 관리**
   - `useUserPoints()`에서 포인트가 부족할 경우, 해당 요청을 실패 큐에 저장하고 예외를 발생시킵니다.
   - `retryFailedRequests()` 메서드를 통해 큐에 저장된 요청을 재시도합니다. 사용자의 포인트가 충분해지면 해당 요청이 처리되며, 여전히 부족한 경우에는 큐에 남습니다.

3. **동기화 메커니즘**
   - `ConcurrentHashMap`을 사용하여 각 사용자에 대한 락을 관리하고, `ConcurrentLinkedQueue`를 사용하여 실패한 요청을 스레드 안전하게 관리합니다.
   - 이는 다중 스레드 환경에서 발생할 수 있는 경합 조건을 방지하고, 포인트 충전 및 사용이 일관되게 처리되도록 보장합니다.

---

## 동시성 테스트

### 1. 여러회원이_한번에_포인트충전

```java
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
```
## 여러회원이_동시에_포인트_충전과_사용_번갈아가면서_테스트
```java
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
```

## 랜덤 충전 및 사용 동시성 테스트
```java
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
```

## 포인트 부족 시 다중 스레드 재시도 테스트
```java
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
```

## 포인트 충전 후 사용 순서 보장 테스트
``` java
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

```

## 동시 사용 시 포인트 중복 차감 방지 테스트
```java
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

```