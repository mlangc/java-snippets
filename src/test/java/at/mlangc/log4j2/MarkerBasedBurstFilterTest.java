package at.mlangc.log4j2;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class MarkerBasedBurstFilterTest {
    private static final Marker GLOBAL_ONCE_PER_SEC_BURST1 = MarkerManager.getMarker("globallyThrottled1");
    private static final Marker GLOBAL_TWICE_PER_SEC_BURST2 = MarkerManager.getMarker("globallyThrottled1");
    private static final Marker GLOBAL_ONCE_PER_SEC_BURST2 = MarkerManager.getMarker("globallyThrottled3");

    private static final Marker LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1 = MarkerManager.getMarker("loggerLocallyThrottled1");

    private static final Logger LOG = LogManager.getLogger(MarkerBasedBurstFilterTest.class);
    private static final Logger OTHER_LOG = LogManager.getLogger(MarkerBasedBurstFilterTest.class.getPackageName() + ".Other");

    record MarkerWithSpec(Marker marker, int intervalMillis, int maxLogs) {
        long intervalNanos() {
            return TimeUnit.MILLISECONDS.toNanos(intervalMillis);
        }
    }

    private final MarkerBasedCountingNopAppender countingNopAppender = MarkerBasedCountingNopAppender.getListAppender("CountingNopAppender");

    private final Random random = new Random(313);

    @BeforeEach
    void beforeEach() throws InterruptedException {
        Thread.sleep(2);
        countingNopAppender.resetCounters();
    }

    @Test
    void logsWithoutMarkersShouldNotBeThrottled() {
        final int logLines = 1000;

        for (int i = 0; i < logLines; i++) {
            logRandomInfo(LOG, i, null);
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(MarkerBasedCountingNopAppender.NO_MARKER)).isGreaterThanOrEqualTo(logLines);
    }

    static List<MarkerWithSpec> globalMarkerWithSpecList() {
        return List.of(
                new MarkerWithSpec(GLOBAL_ONCE_PER_SEC_BURST1, 1, 1),
                new MarkerWithSpec(GLOBAL_ONCE_PER_SEC_BURST2, 2, 2),
                new MarkerWithSpec(GLOBAL_TWICE_PER_SEC_BURST2, 1, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("globalMarkerWithSpecList")
    void logsWithGlobalMarkersShouldBeThrottled(MarkerWithSpec markerWithSpec) {
        assumeThat(countingNopAppender.currentCounts().getOrDefault(markerWithSpec.marker, 0L)).isZero();

        var elapsedNanos = repeatConcurrentlyTillElapsed(markerWithSpec.intervalMillis * 10L,
                TimeUnit.MILLISECONDS,
                (i, rng) -> logRandomInfo(i % 2 == 0 ? LOG : OTHER_LOG, i, markerWithSpec.marker, rng));

        var maxTouchedIntervals = (elapsedNanos + markerWithSpec.intervalNanos() - 1) / markerWithSpec.intervalNanos();
        var maxLogs = maxTouchedIntervals * markerWithSpec.maxLogs;

        var counts = countingNopAppender.currentCounts();

        assertThat(counts.get(markerWithSpec.marker))
                .as("elapsedNanos=%s, maxTouchedIntervals=%s, counts=%s, markerWithSpec=%s", elapsedNanos, maxTouchedIntervals, counts, markerWithSpec)
                .isLessThanOrEqualTo(maxLogs);
    }

    @Test
    void throttlingShouldWorkWithLoggerSpecificMarker() {
        final int logLines = 10;

        for (int i = 0; i < logLines; i++) {
            logRandomInfo(LOG, i, LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1);
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1)).isLessThanOrEqualTo(2);
    }

    @Test
    void throttlingShouldNotBeAppliedToUnrelatedLogger() {
        final int logLines = 10;

        for (int i = 0; i < logLines; i++) {
            logRandomInfo(OTHER_LOG, i, LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1);
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1)).isGreaterThanOrEqualTo(logLines);
    }

    private static long repeatConcurrentlyTillElapsed(long time, TimeUnit unit, BiConsumer<Long, Random> op) {
        AtomicBoolean stop = new AtomicBoolean();
        ForkJoinPool.commonPool().schedule(() -> stop.set(true), time, unit);
        long t0 = System.nanoTime();

        final int parallelism = 4;
        var futures = new CompletableFuture[4];
        for (int i = 0; i < parallelism; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                var rng = ThreadLocalRandom.current();
                for (long j = 0; !stop.get() && j < Long.MAX_VALUE; j++) {
                    op.accept(j, rng);
                }
            });
        }

        CompletableFuture.allOf(futures).join();
        return System.nanoTime() - t0;
    }

    private static void logRandomInfo(Logger logger, long i, Marker marker, Random random) {
        switch (random.nextInt(13)) {
            case 0 -> logger.info(marker, "test-msg-" + i);
            case 1 -> logger.info(marker, "test-msg-{}", i);
            case 2 -> logger.info(marker, "test-msg-{}: {}", i, i);
            case 3 -> logger.info(marker, "test-msg-{}: {},{}", i, i, i);
            case 4 -> logger.info(marker, "test-msg-{}: {},{},{}", i, i, i, i);
            case 5 -> logger.info(marker, "test-msg-{}: {},{},{},{}", i, i, i, i, i);
            case 6 -> logger.info(marker, "test-msg-{}: {},{},{},{},{}", i, i, i, i, i, i);
            case 7 -> logger.info(marker, "test-msg-{}: {},{},{},{},{},{}", i, i, i, i, i, i, i);
            case 8 -> logger.info(marker, "test-msg-{}: {},{},{},{},{},{},{}", i, i, i, i, i, i, i, i);
            case 9 -> logger.info(marker, "test-msg-{}: {},{},{},{},{},{},{},{}", i, i, i, i, i, i, i, i, i);
            case 10 -> logger.info(marker, "test-msg-{}: {},{},{},{},{},{},{},{},{}", i, i, i, i, i, i, i, i, i, i);
            case 11 -> logger.info(marker, "test-msg-{}: {},{},{},{},{},{},{},{},{},{}", i, i, i, i, i, i, i, i, i, i, i);
            default-> logger.info(marker, "test-msg-{}", i, new TestException());
        }
    }

    private static class TestException extends RuntimeException {
        TestException() {
            super(null, null, true, false);
        }
    }

    private void logRandomInfo(Logger logger, long i, Marker marker) {
        logRandomInfo(logger, i, marker, random);
    }
}