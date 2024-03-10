package at.mlangc.experiments;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class DifferenceBetweenSeqCstAndAcqRelTest {
    private ExecutorService executor;

    @BeforeEach
    void beforeEach() {
        executor = Executors.newFixedThreadPool(2);
    }


    @AfterEach
    void afterEach() {
        executor.shutdownNow();
    }

    @Test
    void test() {
        for (int i = 0; i < 250_000; i++) {
            var started1 = new AtomicBoolean();
            var started2 = new AtomicBoolean();
            var succeeded1 = new MutableBoolean();
            var succeeded2 = new MutableBoolean();
            var latch = new CountDownLatch(2);


            var updater1 = (Runnable) () -> {
                try {
                    latch.countDown();
                    latch.await();
                    started1.setRelease(true);

                    if (!started2.getAcquire()) {
                        succeeded1.setValue(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            var updater2 = (Runnable) () -> {
                try {
                    latch.countDown();
                    latch.await();
                    started2.setRelease(true);

                    if (!started1.getAcquire()) {
                        succeeded2.setValue(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            var task1 = CompletableFuture.runAsync(updater1, executor);
            var task2 = CompletableFuture.runAsync(updater2, executor);

            assertThat(task1).succeedsWithin(1, TimeUnit.SECONDS);
            assertThat(task2).succeedsWithin(1, TimeUnit.SECONDS);

            assertThat(succeeded1.booleanValue() && succeeded2.booleanValue())
                    .as("i=%s", i)
                    .isFalse();
        }
    }
}
