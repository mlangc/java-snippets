package at.mlangc.experiments;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class PiggybackOnVolatileReadTest {
    private static final VarHandle LAST_UPDATE_OPAQUE;

    static {
        try {
            var lookup = MethodHandles.lookup();
            LAST_UPDATE_OPAQUE = lookup.findVarHandle(PiggybackOnVolatileReadTest.class, "lastUpdateOpaque", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile long lastUpdateVolatile;
    @SuppressWarnings("unused")
    private long lastUpdateOpaque;
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
    @ValueSource(ints = {1, 10, 100, 1000, 10_000})
    void volatileReadShouldPublishEffectsHappenedBeforeVolatileWrite(int arraySize) throws InterruptedException {
        var values = new long[arraySize];
        var stop = new AtomicBoolean();
        var lastUpdateRead = new MutableLong();

        var reader = (Runnable) () -> {
            while (!stop.get()) {
                var lastUpdate = lastUpdateVolatile;
                lastUpdateRead.setValue(lastUpdate);

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
            assumeThat(lastUpdateRead.longValue()).isPositive();
        } finally {
            stop.set(true);
        }
    }


    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 10_000})
    void opaqueReadShouldPublishEffectsBeforeOpaqueWrite(int arraySize) throws InterruptedException {
        var values = new long[arraySize];
        var stop = new AtomicBoolean();
        var lastUpdateRead = new MutableLong();

        var reader = (Runnable) () -> {
            while (!stop.get()) {
                var lastUpdate = (long) LAST_UPDATE_OPAQUE.getOpaque(this);
                lastUpdateRead.setValue(lastUpdate);

                for (int i = 0; i < values.length; i++) {
                    assertThat(values[i]).as("i=%s", i).isGreaterThanOrEqualTo(lastUpdate);
                }
            }
        };

        var writer = (Runnable) () -> {
            while (!stop.get()) {
                var update = (long) LAST_UPDATE_OPAQUE.getOpaque(this) + 1;
                Arrays.fill(values, update);
                LAST_UPDATE_OPAQUE.setOpaque(this, update);
            }
        };

        try {
            var readerTask = CompletableFuture.runAsync(reader, executor);
            var writerTask = CompletableFuture.runAsync(writer, executor);
            Thread.sleep(1000);
            stop.set(true);

            assertThat(readerTask).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(writerTask).succeedsWithin(5, TimeUnit.SECONDS);
            assumeThat(lastUpdateRead.longValue()).isPositive();
        } finally {
            stop.set(true);
        }
    }
}
