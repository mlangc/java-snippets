package at.mlangc.log4j2;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkerBasedBurstFilterTest {
    private static final Marker ONCE_PER_SEC_BURST1 = MarkerManager.getMarker("throttle:1s:1logs");
    private static final Marker ONCE_PER_SEC_BURST2 = MarkerManager.getMarker("throttle:2s:2logs");
    private static final Marker TWICE_PER_SEC_BURST2 = MarkerManager.getMarker("throttle:1s:1logs");

    private static final Logger LOG = LogManager.getLogger(MarkerBasedBurstFilterTest.class);

    record MarkerWithSpec(Marker marker, int intervalSecs, int maxLogs) { }

    @Test
    void logsWithoutMarkersShouldNotBeThrottled() {
        final int logLines = 1000;

        for (int i = 0; i < logLines; i++) {
            LOG.info("test");
        }

        var counts = getMarkerBasedCountsFromTestAppender();
        assertThat(counts.get(MarkerBasedCountingNopAppender.NULL_MARKER)).isGreaterThanOrEqualTo(logLines);
    }

    static List<MarkerWithSpec> markerWithSpecList() {
        return List.of(
                new MarkerWithSpec(ONCE_PER_SEC_BURST1, 1, 1),
                new MarkerWithSpec(ONCE_PER_SEC_BURST2, 2, 2),
                new MarkerWithSpec(TWICE_PER_SEC_BURST2, 1, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("markerWithSpecList")
    void logsWithMarkersAreThrottledAccordingly(MarkerWithSpec markerWithSpec) {
        final int logLines = 1000;

        for (int i = 0; i < logLines; i++) {
            LOG.info(markerWithSpec.marker, "test");
        }

        var counts = getMarkerBasedCountsFromTestAppender();
        assertThat(counts.get(markerWithSpec.marker)).isLessThanOrEqualTo(markerWithSpec.maxLogs * 2);
    }

    private static Map<org.apache.logging.log4j.Marker, Long> getMarkerBasedCountsFromTestAppender() {
        return MarkerBasedCountingNopAppender.getListAppender("Test").currentCounts();
    }
}