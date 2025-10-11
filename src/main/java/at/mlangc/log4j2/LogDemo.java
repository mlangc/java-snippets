package at.mlangc.log4j2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LogDemo {
    private static final Logger LOG = LoggerFactory.getLogger(LogDemo.class);
    private static final Marker THROTTLE_ONE_LOGS_PER_SEC = MarkerFactory.getMarker("throttle:1ps");
    private static final Marker THROTTLE_TWO_LOGS_PER_SEC = MarkerFactory.getMarker("throttle:2ps");

    static void main() throws InterruptedException {
        while (true) {
            LOG.info(THROTTLE_ONE_LOGS_PER_SEC, "Running 1");
            LOG.info(THROTTLE_TWO_LOGS_PER_SEC, "Running 2");
        }
    }
}
