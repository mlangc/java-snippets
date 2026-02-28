package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assumptions.assumeThat;

class SimpleLockTest {
    static ExecutorService executor;

    @BeforeAll
    static void beforeAll() {
        executor = Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory());
    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }

    static List<Class<? extends SimpleLock>> locks() {
        return List.of(
                JavaUtilConcurrentReentrantLock.class,
                CompareAndSetLock.class,
                GetAndSetLock.class,
                ReentrantGetAndSetLock.class,
                ReentrantGetAndSetLockWithBackoff.class);
    }

    @ParameterizedTest
    @MethodSource("locks")
    void shouldProperlyProtectSharedCounter(Class<? extends SimpleLock> clazz) {
        var lock = newLock(clazz);

        var counter = new Object() {
            long value;
        };

        var incrementsPerThread = 1_000_000L;
        var numThreads = 4;

        var jobs = IntStream.range(0, numThreads)
                .mapToObj(_ -> CompletableFuture.runAsync(() -> {
                            for (int i = 0; i < incrementsPerThread; i++) {
                                if (lock.isReentrant() && (i & 1) != 0) {
                                    lock.runWithLock(() -> lock.runWithLock(() -> counter.value++));
                                } else {
                                    lock.runWithLock(() -> counter.value++);
                                }
                            }
                        }, executor)
                ).toList();

        jobs.forEach(job -> assertThat(job).succeedsWithin(5, TimeUnit.SECONDS));
        assertThat(counter.value).isEqualTo(numThreads * incrementsPerThread);
    }

    @ParameterizedTest
    @MethodSource("locks")
    void shouldBeReentrant(Class<? extends SimpleLock> clazz) {
        var lock = newLock(clazz);
        assumeThat(lock.isReentrant()).isTrue();
        assertThatNoException().isThrownBy(() -> lock.runWithLock(() -> lock.runWithLock(() -> { })));
    }

    private static SimpleLock newLock(Class<? extends SimpleLock> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception", e);
        }
    }
}