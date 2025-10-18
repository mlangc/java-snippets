package at.mlangc.log4j2.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("RoutingFilterTest.xml")
class RoutingFilterTest {
    static final Marker COLORED = MarkerManager.getMarker("color");
    static final Marker RED = MarkerManager.getMarker("red").addParents(COLORED);
    static final Marker YELLOW = MarkerManager.getMarker("yellow").addParents(COLORED);
    static final Marker GREEN = MarkerManager.getMarker("green").addParents(COLORED);
    static final Marker BLUE = MarkerManager.getMarker("blue").addParents(COLORED);

    private final ListAppender throttledListAppender;
    private final LoggerContext loggerContext;

    RoutingFilterTest(LoggerContext loggerContext, @Named("ThrottledListAppender") ListAppender throttledListAppender) {
        this.loggerContext = loggerContext;
        this.throttledListAppender = throttledListAppender;
    }

    @BeforeEach
    void beforeEach() {
        throttledListAppender.clear();
    }

    @Test
    void logsShouldBeFilteredBasedOnTheirColorMarker() {
        var log = loggerContext.getLogger(getClass().getCanonicalName() + ".Colored");

        log.info(RED, "red lips");
        log.info(GREEN, "green grass");
        log.info(BLUE, "blue sky");
        log.info(YELLOW, "yellow submarine");


        assertThat(throttledListAppender.getEvents())
                .filteredOn(e -> e.getMarker().isInstanceOf(COLORED))
                .hasSize(4);

        throttledListAppender.clear();
        log.info(RED, "blue lips");
        log.info(GREEN, "brown grass");
        log.info(BLUE, "red sky");
        log.info(YELLOW, "blue submarine");
        assertThat(throttledListAppender.getEvents()).isEmpty();

        log.info("all is grey");
        log.info("black as midnight on a moonless night");
        assertThat(throttledListAppender.getEvents()).hasSize(2);

        throttledListAppender.clear();
        log.info("colors, give me colors");
        assertThat(throttledListAppender.getEvents()).isEmpty();
    }
}
