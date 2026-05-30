package at.mlangc.concurrent.task.dispatcher;

import org.apache.commons.lang3.mutable.MutableInt;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


class SynchronizingTaskDispatcherTest {
    static ExecutorService executor;

    @BeforeAll
    static void beforeAll() {
        executor = Executors.newWorkStealingPool(4);
    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }


    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldExecuteTrivialTasks(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var f1 = dispatcher.dispatch(() -> 1);
        var f2 = dispatcher.dispatch(0, () -> 2);
        var f3 = dispatcher.dispatch(Set.of(0, 1), () -> 3);

        assertThat(f1).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(1);
        assertThat(f2).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(2);
        assertThat(f3).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldProperlySerializeTasksInSimpleExample(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var task1Counter = new MutableInt();
        var task2Counter = new MutableInt();
        var targetCount = 50_000;

        Supplier<Integer> task1 = task1Counter::getAndIncrement;
        Supplier<Integer> task2 = task2Counter::getAndIncrement;

        var task1Futures = new HashMap<Integer, CompletableFuture<Integer>>();
        var task2Futures = new HashMap<Integer, CompletableFuture<Integer>>();
        for (var i = 0; i < targetCount; i++) {
            task1Futures.put(i, dispatcher.dispatch(1, task1));
            task2Futures.put(i, dispatcher.dispatch(2, task2));
        }

        assertThat(task1Futures.entrySet())
                .allSatisfy(entry -> assertThat(entry.getValue()).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(entry.getKey()));

        assertThat(task2Futures.entrySet())
                .allSatisfy(entry -> assertThat(entry.getValue()).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(entry.getKey()));

        assertThat(task1Counter.get()).isEqualTo(targetCount);
        assertThat(task2Counter.get()).isEqualTo(targetCount);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldRespectOrderForSingleSynchronizer(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var numTasks = 10_000;
        var counter = new MutableInt();

        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numTasks; i++) {
            futures.add(
                    dispatcher.dispatch(0, () -> {
                        assertThat(counter.intValue()).isZero();
                        counter.increment();
                        assertThat(counter.intValue()).isOne();
                        return null;
                    })
            );

            futures.add(
                    dispatcher.dispatch(0, () -> {
                        assertThat(counter.intValue()).isOne();
                        counter.decrement();
                        assertThat(counter.intValue()).isZero();
                        return null;
                    })
            );
        }

        assertThat(CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))).succeedsWithin(5, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldProperlySerializeTasksInMoreElaborateExample(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var numSynchronizers = 8;
        var setCounters = new long[1 << numSynchronizers];
        var individualCounters = new long[numSynchronizers];
        var numTasks = 100_000;
        var rng = new Random(23);

        var futures = new ArrayList<CompletableFuture<Void>>();
        for (var i = 0; i < numTasks; i++) {
            var synchronizersId = rng.nextInt(setCounters.length);
            var synchronizers = IntStream.range(0, numSynchronizers)
                    .filter(bit -> ((1 << bit) & synchronizersId) != 0)
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());

            IntFunction<Supplier<Void>> newTaskWithDelta = delta -> () -> {
                for (var s : synchronizers) {
                    assertThat(individualCounters[s]).isNotNegative();
                    individualCounters[s] += delta;
                    assertThat(individualCounters[s]).isNotNegative();
                }

                if (synchronizersId != 0) {
                    assertThat(setCounters[synchronizersId]).as("synchronizers=%s", synchronizers).isNotNegative();
                    setCounters[synchronizersId] += delta;
                    assertThat(setCounters[synchronizersId]).as("synchronizers=%s", synchronizers).isNotNegative();
                }

                return null;
            };

            var delta = rng.nextInt(10);
            futures.add(dispatcher.dispatch(synchronizers, newTaskWithDelta.apply(delta)));
            futures.add(dispatcher.dispatch(synchronizers, newTaskWithDelta.apply(-delta)));
        }

        assertThat(CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))).succeedsWithin(5, TimeUnit.SECONDS);

        assertThat(Arrays.stream(individualCounters)).allMatch(c -> c >= 0);
        assertThat(Arrays.stream(setCounters)).allMatch(c -> c >= 0);
    }


    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldProperlySynchronizeExecutionWhenCalledFromMultipleThreads(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var list0 = new ArrayList<Integer>();
        var list1 = new ArrayList<Integer>();

        var start0 = -25_000;
        var start1 = 0;

        list0.add(start0);
        list1.add(start1);

        var tasksPerJob = 10_000;
        var numJobs = 5;

        IntFunction<Supplier<CompletableFuture<Void>>> mkJob = seed -> () -> {
            var rng = new Random(seed);

            return CompletableFuture.allOf(
                    IntStream.range(0, tasksPerJob)
                            .mapToObj(_ -> {
                                var synchronizerBits = rng.nextInt(1, 4);
                                var synchronizers = IntStream.range(0, 4)
                                        .filter(bit -> ((1 << bit) & synchronizerBits) != 0)
                                        .boxed()
                                        .collect(Collectors.toUnmodifiableSet());

                                Runnable task = () -> {
                                    for (var s : synchronizers) {
                                        var list = s == 0 ? list0 : (s == 1) ? list1 : null;

                                        if (list != null) {
                                            list.add(list.getLast() + 1);
                                        }
                                    }
                                };

                                return dispatcher.dispatch(synchronizers, task);
                            }).toArray(CompletableFuture<?>[]::new));
        };

        var allJobs = IntStream.range(0, numJobs)
                .mapToObj(i -> CompletableFuture.supplyAsync(mkJob.apply(i), executor))
                .map(f -> f.thenCompose(Function.identity()))
                .toArray(CompletableFuture<?>[]::new);

        assertThat(CompletableFuture.allOf(allJobs)).succeedsWithin(5, TimeUnit.SECONDS);

        assertThat(List.of(list0, list1))
                .allSatisfy(list -> {
                    for (int i = 1; i < list.size(); i++) {
                        assertThat(list.get(i)).isEqualTo(list.get(i - 1) + 1);
                    }
                });
    }

    @Test
    void shouldRespectMaxTasksInFlight() throws InterruptedException {
        var maxTasksInFlight = 17;
        var queue = new ArrayBlockingQueue<Runnable>(maxTasksInFlight * 10);
        var poolSize = 1;

        try (var executor = new ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.MINUTES, queue)) {
            var dispatcher = new SynchronizingTaskDispatcher<String>(maxTasksInFlight);
            var lock = new ReentrantLock();
            lock.lock();
            try {
                Runnable dispatchJob = () -> {
                    lock.lock();
                    lock.unlock();
                };

                Supplier<CompletableFuture<?>> submitDispatchJobs = () ->
                        CompletableFuture.allOf(
                                IntStream.range(0, maxTasksInFlight * 2)
                                        .mapToObj(_ -> dispatcher.dispatchAsync(AsyncTask.of(dispatchJob, executor)))
                                        .toArray(CompletableFuture<?>[]::new));

                var submitJob = CompletableFuture.supplyAsync(submitDispatchJobs).thenCompose(f -> f);
                Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(queue).hasSize(maxTasksInFlight - poolSize));
                Thread.sleep(10);
                assertThat(queue).hasSize(maxTasksInFlight - poolSize);

                lock.unlock();
                assertThat(submitJob).succeedsWithin(1, TimeUnit.SECONDS);
            } finally {
                while (lock.isLocked()) {
                    lock.unlock();
                }
            }

        }
    }

    @Test
    void shouldRespectMaxTasksInFlightEvenWhenExecuteReturnsFailedFutureOrThrows() throws InterruptedException {
        var maxTasksInFlight = 17;
        var numThrowingTasks = 3;
        var numFailingTasks = 3;
        var poolSize = 1;
        var queue = new ArrayBlockingQueue<Runnable>(maxTasksInFlight * 10);

        try (var executor = new ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.MINUTES, queue)) {
            var dispatcher = new SynchronizingTaskDispatcher<String>(maxTasksInFlight);

            AsyncTask<Void> throwingTask = () -> { throw new RuntimeException(":-O"); };
            assertThat(IntStream.range(0, numThrowingTasks).mapToObj(_ -> dispatcher.dispatchAsync(throwingTask)))
                    .allSatisfy(f -> assertThat(f).failsWithin(1, TimeUnit.SECONDS));

            AsyncTask<Void> failingTask = AsyncTask.of(() -> { throw new RuntimeException(":-("); });
            assertThat(IntStream.range(0, numFailingTasks).mapToObj(_ -> dispatcher.dispatchAsync(failingTask)))
                    .allSatisfy(f -> assertThat(f).failsWithin(1, TimeUnit.SECONDS));

            var lock = new ReentrantLock();
            lock.lock();
            try {
                Runnable dispatchJob = () -> {
                    lock.lock();
                    lock.unlock();
                };

                Supplier<CompletableFuture<?>> submitDispatchJobs = () ->
                        CompletableFuture.allOf(
                                IntStream.range(0, maxTasksInFlight * 2)
                                        .mapToObj(_ -> dispatcher.dispatchAsync(AsyncTask.of(dispatchJob, executor)))
                                        .toArray(CompletableFuture<?>[]::new));

                var submitJob = CompletableFuture.supplyAsync(submitDispatchJobs).thenCompose(f -> f);
                Awaitility.await().atMost(1, TimeUnit.SECONDS)
                        .untilAsserted(() -> assertThat(queue).hasSizeGreaterThanOrEqualTo(maxTasksInFlight - poolSize));
                Thread.sleep(10);
                assertThat(queue).hasSize(maxTasksInFlight - poolSize);

                lock.unlock();
                assertThat(submitJob).succeedsWithin(1, TimeUnit.SECONDS);
            } finally {
                while (lock.isLocked()) {
                    lock.unlock();
                }
            }
        }
    }

    @Test
    void shouldNotRunIntoOomsNeedlessly() {
        var dispatcher = new SynchronizingTaskDispatcher<Integer>(5000);
        var largeArray = new byte[1 << 19];
        var totalAlloc = 1L << 35;
        var numTasks = Math.toIntExact(totalAlloc / largeArray.length);

        Supplier<byte[]> allocatingJob = largeArray::clone;
        var keepAlive = new ArrayList<Integer>();

        var allocatingJobs = CompletableFuture.allOf(
                IntStream.range(0, numTasks)
                        .boxed()
                        .peek(keepAlive::add)
                        .map(l -> dispatcher.dispatch(l, allocatingJob).thenApply(_ -> null))
                        .toArray(CompletableFuture<?>[]::new));

        assertThat(keepAlive).hasSize(numTasks);
        assertThat(allocatingJobs).succeedsWithin(5, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void errorsShouldBePropagatedProperly(SynchronizingTaskDispatcher<Integer> dispatcher) {
        String errMsg = ":-O";
        AsyncTask<Void> throwingTask = () -> { throw new RuntimeException(errMsg); };
        AsyncTask<Void> failingTask = AsyncTask.of(() -> { throw new RuntimeException(errMsg); });

        assertThat(dispatcher.dispatchAsync(throwingTask))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableThat().havingRootCause()
                .withMessage(errMsg);

        assertThat(dispatcher.dispatchAsync(failingTask))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableThat().havingRootCause()
                .withMessage(errMsg);
    }

    /**
     * Probes the {@link java.util.WeakHashMap} that backs the synchronizer -> chain map. Synchronizer keys are compared
     * by {@code equals}, but callers may legitimately hand in distinct-but-equal key objects on successive dispatches
     * (e.g. freshly boxed {@code Integer}s, {@code new String(...)}, value objects). When that happens the map entry stays
     * keyed by the <em>first</em> key object (that is how {@code WeakHashMap.put} updates an existing entry), while the
     * value (the live chain) only keeps the <em>latest</em> key reachable. Once the first task completes, its key object
     * can be collected, the entry is expunged, and a later dispatch on an equal synchronizer no longer chains behind the
     * still running task -- breaking serialization.
     */
    @Test
    void shouldKeepSerializingWhenEarlierSynchronizerKeyObjectIsGarbageCollected() throws Exception {
        var taskExecutor = Executors.newFixedThreadPool(4);
        try {
            var dispatcher = new SynchronizingTaskDispatcher<String>(100);

            var t1Started = new CountDownLatch(1);
            var t1Release = new CountDownLatch(1);
            var t2Started = new CountDownLatch(1);
            var t2Release = new CountDownLatch(1);
            var t2Active = new AtomicBoolean(false);
            var t3RanWhileT2Active = new AtomicBoolean(false);

            // T1 blocks; we keep only a WeakReference to its (non-interned) synchronizer key.
            var key1 = new String("sync");
            var key1Ref = new WeakReference<>(key1);
            var f1 = dispatcher.dispatchAsync(key1, AsyncTask.of(() -> {
                t1Started.countDown();
                awaitUninterruptibly(t1Release);
                return 1;
            }, taskExecutor));
            //noinspection UnusedAssignment
            key1 = null;

            assertThat(t1Started.await(2, TimeUnit.SECONDS)).isTrue();

            // T2 uses an equal-but-distinct key; it chains behind T1, then blocks while flagging itself active.
            var f2 = dispatcher.dispatchAsync(new String("sync"), AsyncTask.of(() -> {
                t2Active.set(true);
                t2Started.countDown();
                awaitUninterruptibly(t2Release);
                t2Active.set(false);
                return 2;
            }, taskExecutor));

            // Let T1 finish. Its key object now has no strong referrer, yet the map entry is still keyed by it and its
            // value is T2's (still running) chain.
            t1Release.countDown();
            assertThat(f1).succeedsWithin(2, TimeUnit.SECONDS);
            assertThat(t2Started.await(2, TimeUnit.SECONDS)).isTrue();

            // Best-effort nudge for the GC to reclaim the first key object. With a buggy WeakHashMap backing the chains
            // this expunges the still-needed entry; with a correct (strong-keyed) map the entry keeps the key reachable
            // and nothing is collected. We do NOT assert that it was collected -- that would only hold for the buggy
            // variant. The serialization assertions below are what must hold regardless.
            for (var i = 0; i < 10 && key1Ref.get() != null; i++) {
                System.gc();
                Thread.sleep(20);
            }

            // T3 uses the same synchronizer value -> it must wait for T2, which is still running (blocked).
            var f3 = dispatcher.dispatchAsync(new String("sync"), AsyncTask.of(() -> {
                if (t2Active.get()) {
                    t3RanWhileT2Active.set(true);
                }
                return 3;
            }, taskExecutor));

            Thread.sleep(200);
            var f3DoneWhileT2Blocked = f3.isDone();

            t2Release.countDown();
            assertThat(f2).succeedsWithin(2, TimeUnit.SECONDS);
            assertThat(f3).succeedsWithin(2, TimeUnit.SECONDS);

            assertThat(f3DoneWhileT2Blocked)
                    .as("T3 finished while T2 (sharing its synchronizer) was still running")
                    .isFalse();
            assertThat(t3RanWhileT2Active)
                    .as("T3 ran concurrently with T2 despite sharing a synchronizer")
                    .isFalse();
        } finally {
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void tasksWithNonIntersectingSynchronizersDoNotBlockEachOther() throws InterruptedException {
        var dispatcher = new SynchronizingTaskDispatcher<Integer>(1000);

        var counter0 = new AtomicInteger();
        var counter1 = new MutableInt();
        var counter2 = new MutableInt();
        var latch = new Semaphore(1);
        try {
            latch.acquire();
            var incCounter1Blocked = dispatcher.dispatch(1, () -> {
                latch.acquireUninterruptibly();
                counter1.increment();
                latch.release();
            });

            var incCounter0 = dispatcher.dispatch(counter0::incrementAndGet);
            var incCounter2 = dispatcher.dispatch(2, counter2::incrementAndGet);

            assertThat(incCounter0).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(1);
            assertThat(incCounter2).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(1);

            var incCounter12Blocked = dispatcher.dispatch(Set.of(1, 2), () -> {
                counter1.increment();
                counter2.increment();
            });

            assertThat(incCounter1Blocked).isNotDone();
            assertThat(incCounter12Blocked).isNotDone();

            latch.release();
            assertThat(incCounter1Blocked).succeedsWithin(1, TimeUnit.SECONDS);
            assertThat(incCounter12Blocked).succeedsWithin(1, TimeUnit.SECONDS);

            assertThat(counter1.intValue()).isEqualTo(counter2.intValue()).isEqualTo(2);
        } finally {
            if (latch.availablePermits() == 0) {
                latch.release();
            }
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    static List<SynchronizingTaskDispatcher<Integer>> synchronizingIntegerTaskDispatchers() {
        return List.of(new SynchronizingTaskDispatcher<>(500), new SynchronizingTaskDispatcher<>(50), new SynchronizingTaskDispatcher<>(5));
    }
}