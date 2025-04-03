package at.mlangc.art.of.multiprocessor.programming;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class PetersonLockTest {
    private final PetersonLock sut = new PetersonLock();
    private final ExecutorService executor = Executors.newCachedThreadPool(
            Thread.ofPlatform().name("test-thread:", 0).factory());

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
    void shouldThrowIfMoreThan2ThreadsAreEncountered() {
        var latch = new CountDownLatch(3);

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

        var jobs = IntStream.range(0, 3)
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

        final var incrementsPerThread = 1_000_000;
        Runnable incrementCounterWithLock = () -> {
            for (int i = 0; i < incrementsPerThread; i++) {
                sut.lock();
                counter.value++;
                sut.unlock();
            }
        };

        var job1 = CompletableFuture.runAsync(incrementCounterWithLock, executor);
        var job2 = CompletableFuture.runAsync(incrementCounterWithLock, executor);

        assertThat(job1).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(job2).succeedsWithin(1, TimeUnit.SECONDS);

        assertThat(counter.value).isEqualTo(2 * incrementsPerThread);
    }

    private static void doWithThread0(Runnable op) {
        String oldName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("test-thread:0");
            op.run();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}