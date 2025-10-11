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

import static org.assertj.core.api.Assertions.assertThat;

class MarkerBasedBurstFilterTest {
    private static final Marker GLOBAL_ONCE_PER_SEC_BURST1 = MarkerManager.getMarker("global:throttle:1s:1logs");
    private static final Marker GLOBAL_ONCE_PER_SEC_BURST2 = MarkerManager.getMarker("global:throttle:2s:2logs");
    private static final Marker GLOBAL_TWICE_PER_SEC_BURST2 = MarkerManager.getMarker("global:throttle:1s:1logs");

    private static final Marker LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1 = MarkerManager.getMarker("logger-specific:throttle:1s:1logs");

    private static final Logger LOG = LogManager.getLogger(MarkerBasedBurstFilterTest.class);
    private static final Logger OTHER_LOG = LogManager.getLogger(MarkerBasedBurstFilterTest.class.getCanonicalName() + ".Other");

    record MarkerWithSpec(Marker marker, int intervalSecs, int maxLogs) { }

    private final MarkerBasedCountingNopAppender countingNopAppender = MarkerBasedCountingNopAppender.getListAppender("CountingNopAppender");

    @BeforeEach
    void beforeEach() throws InterruptedException {
        countingNopAppender.resetCounters();
        Thread.sleep(2);
    }

    @Test
    void logsWithoutMarkersShouldNotBeThrottled() {
        final int logLines = 1000;

        for (int i = 0; i < logLines; i++) {
            LOG.info("test");
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
        final int logLines = 10;

        for (int i = 0; i < logLines; i++) {
            (i % 2 == 0 ? LOG : OTHER_LOG).info(markerWithSpec.marker, "test");
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(markerWithSpec.marker)).isLessThanOrEqualTo(markerWithSpec.maxLogs * 2);
    }

    @Test
    void throttlingShouldWorkWithLoggerSpecificMarker() {
        final int logLines = 10;

        for (int i = 0; i < logLines; i++) {
            LOG.info(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1, "test");
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1)).isLessThanOrEqualTo(2);
    }

    @Test
    void throttlingShouldNotBeAppliedToUnrelatedLogger() {
        final int logLines = 10;

        for (int i = 0; i < logLines; i++) {
            OTHER_LOG.info(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1, "test");
        }

        var counts = countingNopAppender.currentCounts();
        assertThat(counts.get(LOGGER_SPECIFIC_ONCE_PER_SEC_BURST1)).isGreaterThanOrEqualTo(logLines);
    }

}