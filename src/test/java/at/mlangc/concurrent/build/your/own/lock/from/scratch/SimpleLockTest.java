package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
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

    enum LockImpl {
        JAVA_UTIL_REENTRANT_LOCK(JavaUtilConcurrentReentrantLock::new),
        COMPARE_AND_SET_LOCK(CompareAndSetLock::new),
        GET_AND_SET_LOCK(GetAndSetLock::new),
        REENTRANT_GET_AND_SET_LOCK_WITH_BACKOFF(ReentrantGetAndSetLockWithBackoff::new),
        CLH_QUEUE_LOCK(ClhQueueLock::new),
        CLH_QUEUE_LOCK_WITH_HASHMAP(ClhQueueWithHashMapLock::new),
        FANCY_CLH_QUEUE_LOCK(FancyClhQueueLock::new),
        MCS_LOCK(McsLock::new);

        final Supplier<SimpleLock> factory;

        LockImpl(Supplier<SimpleLock> factory) {
            this.factory = factory;
        }
    }

    static List<Class<? extends SimpleLock>> locks() {
        return List.of(
                JavaUtilConcurrentReentrantLock.class,
                CompareAndSetLock.class,
                GetAndSetLock.class,
                ReentrantGetAndSetLock.class,
                ReentrantGetAndSetLockWithBackoff.class,
                ClhQueueLock.class,
                ClhQueueWithHashMapLock.class,
                FancyClhQueueLock.class,
                McsLock.class);
    }

    @ParameterizedTest
    @EnumSource
    void shouldProperlyProtectSharedCounter(LockImpl impl) {
        var lock = impl.factory.get();
        shouldProperlyProtectSharedCounter(lock);

        var reentrantLock = SimpleLocks.makeReentrantAndChecked(lock);
        if (reentrantLock != lock) {
            shouldProperlyProtectSharedCounter(lock);
        }
    }

    private void shouldProperlyProtectSharedCounter(SimpleLock lock) {
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

                                if (lock.hasCheckedUnlock() & (i & 0xFF) == 0xFF) {
                                    assertThatExceptionOfType(IllegalMonitorStateException.class).isThrownBy(lock::unlock);
                                }
                            }
                        }, executor)
                ).toList();

        jobs.forEach(job -> assertThat(job).succeedsWithin(5, TimeUnit.SECONDS));
        assertThat(counter.value).isEqualTo(numThreads * incrementsPerThread);
    }

    @ParameterizedTest
    @EnumSource
    void makeReentrantAndCheckedShouldWork(LockImpl impl) {
        var lock = SimpleLocks.makeReentrantAndChecked(impl.factory.get());
        assumeThat(lock.isReentrant()).isTrue();
        assertThat(lock.hasCheckedUnlock()).isTrue();
        assertThatNoException().isThrownBy(() -> lock.runWithLock(() -> lock.runWithLock(() -> { })));
        assertThatExceptionOfType(IllegalMonitorStateException.class).isThrownBy(lock::unlock);
    }

    @ParameterizedTest
    @EnumSource
    void reentrantLocksShouldBeReentrant(LockImpl impl) {
        var lock = impl.factory.get();
        assumeThat(lock.isReentrant()).isTrue();
        assertThatNoException().isThrownBy(() -> lock.runWithLock(() -> lock.runWithLock(() -> { })));
    }

    @ParameterizedTest
    @EnumSource
    void checkedLocksShouldBeChecked(LockImpl impl) {
        var lock = impl.factory.get();
        assumeThat(lock.hasCheckedUnlock()).isTrue();
        assertThatExceptionOfType(IllegalMonitorStateException.class).isThrownBy(lock::unlock);
    }
}