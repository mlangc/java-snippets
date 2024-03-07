package at.mlangc.experiments;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class PiggybackOnVolatileReadTest {
    private volatile long lastUpdateVolatile;
    private long lastUpdatePlain;
    private ExecutorService executor;

    @BeforeEach
    void beforeEach() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void afterEach() {
        executor.shutdownNow();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 100_000_000 / 8})
    void volatileReadShouldPublishEffectsBeforeVolatileWrite(int arraySize) throws InterruptedException {
        var values = new long[arraySize];
        var stop = new AtomicBoolean();

        var reader = (Runnable) () -> {
            while (!stop.get()) {
                var lastUpdate = this.lastUpdateVolatile;
                for (int i = 0; i < values.length; i++) {
                    assertThat(values[i]).as("i=%s", i).isGreaterThanOrEqualTo(lastUpdate);
                }
            }
        };

        var writer = (Runnable) () -> {
            while (!stop.get()) {
                var update = lastUpdateVolatile + 1;
                Arrays.fill(values, update);
                lastUpdateVolatile = update;
            }
        };

        try {
            var readerTask = CompletableFuture.runAsync(reader, executor);
            var writerTask = CompletableFuture.runAsync(writer, executor);
            Thread.sleep(1000);
            stop.set(true);

            assertThat(readerTask).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(writerTask).succeedsWithin(5, TimeUnit.SECONDS);
        } finally {
            stop.set(true);
        }
    }


    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 100_000_000 / 8})
    void plainReadShouldNotPublishEffectsBeforePlainWrite(int arraySize) throws InterruptedException {
        var values = new long[arraySize];
        var stop = new AtomicBoolean();

        var reader = (Runnable) () -> {
            while (!stop.get()) {
                var lastUpdate = this.lastUpdatePlain;
                for (int i = 0; i < values.length; i++) {
                    assertThat(values[i]).as("i=%s", i).isGreaterThanOrEqualTo(lastUpdate);
                }
            }
        };

        var writer = (Runnable) () -> {
            while (!stop.get()) {
                var update = lastUpdatePlain + 1;
                Arrays.fill(values, update);
                lastUpdatePlain = update;
            }
        };

        try {
            var readerTask = CompletableFuture.runAsync(reader, executor);
            var writerTask = CompletableFuture.runAsync(writer, executor);
            Thread.sleep(1000);
            stop.set(true);

            assertThat(readerTask).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(writerTask).succeedsWithin(5, TimeUnit.SECONDS);
        } finally {
            stop.set(true);
        }
    }
}
