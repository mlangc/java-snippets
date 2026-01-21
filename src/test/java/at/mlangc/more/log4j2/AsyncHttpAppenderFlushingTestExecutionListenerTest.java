package at.mlangc.more.log4j2;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncHttpAppenderFlushingTestExecutionListenerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncHttpAppenderFlushingTestExecutionListenerTest.class);

    @Test
    void loggingTest() {
        for (int i = 0; i < 1000; i++) {
            LOG.info("l={}", i);
        }

        LOG.info("Done");
    }
}
