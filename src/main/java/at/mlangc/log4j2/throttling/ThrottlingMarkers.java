package at.mlangc.log4j2.throttling;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class ThrottlingMarkers {
    static final Marker AT_MOST_100_LOGS_IN_10_SECS = MarkerFactory.getMarker("atMost100LogsIn10Secs");
    static final Marker AT_MOST_10_LOGS_IN_10_SECS = MarkerFactory.getMarker("atMost10LogsIn10Secs");
    static final Marker AT_MOST_10_LOGS_IN_100_SECS = MarkerFactory.getMarker("atMost10LogsIn100Secs");
}
