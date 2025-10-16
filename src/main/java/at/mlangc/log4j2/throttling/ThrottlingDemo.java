package at.mlangc.log4j2.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrottlingDemo {
    private static final Logger LOG = LoggerFactory.getLogger(ThrottlingDemo.class);

    @SuppressWarnings("LoggingSimilarMessage")
    static void main() {
        while (true) {
            LOG.info(ThrottlingMarkers.AT_MOST_100_LOGS_IN_10_SECS, "Logging with {}", ThrottlingMarkers.AT_MOST_100_LOGS_IN_10_SECS.getName());
            LOG.info(ThrottlingMarkers.AT_MOST_10_LOGS_IN_10_SECS, "Logging with {}", ThrottlingMarkers.AT_MOST_10_LOGS_IN_10_SECS.getName());
            LOG.info(ThrottlingMarkers.AT_MOST_10_LOGS_IN_100_SECS, "Logging with {}", ThrottlingMarkers.AT_MOST_10_LOGS_IN_100_SECS.getName());
        }
    }
}
