package at.mlangc.log4j2;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class ThrottlingMarkers {
    static final Marker ONCE_PER_SECOND = MarkerFactory.getMarker("throttledAt1LogPerSec");
    static final Marker TEN_PER_SECOND = MarkerFactory.getMarker("throttledAt10LogsPerSec");
    static final Marker HUNDRED_PER_SECOND = MarkerFactory.getMarker("throttledAt10LogsPerSec");
}
