package at.mlangc.benchmarks;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SlottedCounterImplsTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean stop = false;

    record SlottedCounterFactory(String name, IntFunction<SlottedCounterImpls.SlottedCounter> ctor) {
        SlottedCounterFactory(Class<? extends SlottedCounterImpls.SlottedCounter> clazz, IntFunction<SlottedCounterImpls.SlottedCounter> ctor) {
            this(clazz.getSimpleName(), ctor);
        }
    }

    static class SlottedCounterUsingAtomicArrayW1 extends SlottedCounterImpls.SlottedCounterUsingAtomicArray {
        SlottedCounterUsingAtomicArrayW1(int numSlots) {
            super(numSlots, 1);
        }
    }

    static class SlottedCounterUsingAtomicArrayW8 extends SlottedCounterImpls.SlottedCounterUsingAtomicArray {
        SlottedCounterUsingAtomicArrayW8(int numSlots) {
            super(numSlots, 8);
        }
    }

    static List<SlottedCounterFactory> slottedCounterFactories() {
        return List.of(
                new SlottedCounterFactory(SlottedCounterImpls.SlottedCounterUsingRentrantLock.class, SlottedCounterImpls.SlottedCounterUsingRentrantLock::new),
                new SlottedCounterFactory(SlottedCounterImpls.SlottedCounterUsingAtomicGuard.class, SlottedCounterImpls.SlottedCounterUsingAtomicGuard::new),
                new SlottedCounterFactory(SlottedCounterUsingAtomicArrayW1.class, SlottedCounterUsingAtomicArrayW1::new),
                new SlottedCounterFactory(SlottedCounterUsingAtomicArrayW8.class, SlottedCounterUsingAtomicArrayW8::new)
        );
    }

    static List<ConsistencyTestCase> consistencyTestCases() {
        var testCases = new ArrayList<ConsistencyTestCase>();
        for (int numSlots : List.of(1, 8)) {
            for (int numThreads : List.of(1, 8)) {
                for (SlottedCounterFactory factory : slottedCounterFactories()) {
                    testCases.add(new ConsistencyTestCase(numThreads, numSlots, factory));
                }
            }
        }

        return testCases;
    }

    record ConsistencyTestCase(int numThreads, int numSlots, SlottedCounterFactory factory) { }

    @ParameterizedTest
    @MethodSource("consistencyTestCases")
    void shouldBeConsistentWithLongAdder(ConsistencyTestCase testCase) {
        var longAdder = new LongAdder();
        var slottedCounter = testCase.factory.ctor.apply(testCase.numSlots);

        Runnable incrementUntilStopped = () -> {
            while (!stop) {
                longAdder.increment();
                slottedCounter.increment();
            }
        };

        var jobs = IntStream.range(0, testCase.numThreads)
                .mapToObj(ignore -> CompletableFuture.runAsync(incrementUntilStopped, executor))
                .toArray(CompletableFuture<?>[]::new);


        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(longAdder.longValue()).isGreaterThan(100));

        stop = true;
        assertThat(CompletableFuture.allOf(jobs)).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(slottedCounter.sum()).isEqualTo(longAdder.sum());
    }

    @AfterEach
    void afterEach() throws InterruptedException {
        stop = true;
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }
}