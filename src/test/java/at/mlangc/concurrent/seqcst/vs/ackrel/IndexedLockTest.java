package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;

abstract class IndexedLockTest {
    private final IndexedLock sut = newLock();
    private final ExecutorService executor = ThreadIndex.newIndexedCachedThreadPool();

    abstract IndexedLock newLock();

    @AfterEach
    void afterEach() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void lockUnlockShouldNotThrow() {
        doWithThread0(() ->
                assertThatNoException().isThrownBy(() -> {
                    sut.lock();
                    sut.unlock();
                }));
    }

    @Test
    void unlockWithoutLockShouldThrow() {
        doWithThread0(() -> assertThatRuntimeException().isThrownBy(sut::unlock));
    }

    @Test
    void lockShouldBeReentrant() {
        assertThatNoException().isThrownBy(() -> doWithThread0(() -> {
            sut.lock();
            sut.lock();
            sut.unlock();
        }));
    }

    @Test
    void shouldThrowIfMoreThanThreadLimitThreadsAreEncountered() {
        assumeThat(sut.threadLimit()).isLessThan(Integer.MAX_VALUE);

        var latch = new CountDownLatch(sut.threadLimit() + 1);
        Runnable waitForLatchLockUnlock = () -> {
            try {
                latch.countDown();
                latch.await();
                sut.lock();
                sut.unlock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(e);
            }
        };

        var jobs = IntStream.range(0, sut.threadLimit() + 1)
                .mapToObj(ignore -> CompletableFuture.runAsync(waitForLatchLockUnlock, executor))
                .toList();

        assertThat(jobs)
                .anySatisfy(job -> assertThat(job).completesExceptionallyWithin(1, TimeUnit.SECONDS));
    }

    @Test
    void shouldProtectSharedCounter() {
        var counter = new Object() {
            int value = 0;
        };

        final var incrementsPerThread = 2_000_000 / maxThreads();
        Runnable incrementCounterWithLock = () -> {
            for (int i = 0; i < incrementsPerThread; i++) {
                sut.lock();
                counter.value++;
                sut.unlock();
            }
        };

        var jobs = IntStream.range(0, maxThreads())
                .mapToObj(ignore -> CompletableFuture.runAsync(incrementCounterWithLock, executor))
                .toList();

        assertThat(jobs).allSatisfy(job -> assertThat(job).succeedsWithin(2, TimeUnit.SECONDS));
        assertThat(counter.value).isEqualTo(maxThreads() * incrementsPerThread);
    }

    @Test
    void shouldProtectSharedTestState() {
        var testState = new TestState();

        final var updatesPerThread = 10_000 / maxThreads();
        Runnable updateAndCheckWithLock = () -> {
            for (int i = 0; i < updatesPerThread; i++) {
                sut.lock();
                try {
                    testState.update();
                    testState.assertConsistent();
                } finally {
                    sut.unlock();
                }
            }
        };

        var jobs = IntStream.range(0, maxThreads())
                .mapToObj(ignore -> CompletableFuture.runAsync(updateAndCheckWithLock, executor))
                .toList();

        assertThat(jobs).allSatisfy(job -> assertThat(job).succeedsWithin(2, TimeUnit.SECONDS));
        testState.assertConsistent(updatesPerThread * maxThreads());
    }

    private int maxThreads() {
        return Math.min(8, sut.threadLimit());
    }

    private static void doWithThread0(Runnable op) {
        String oldName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(ThreadIndex.toName(0));
            op.run();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}