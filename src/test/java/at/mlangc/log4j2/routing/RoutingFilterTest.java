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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoutingFilterTest {
    private static final Logger LOG = LogManager.getLogger(RoutingFilterTest.class);

    static final Marker COLORED = MarkerManager.getMarker("color");
    static final Marker RED = MarkerManager.getMarker("red").addParents(COLORED);
    static final Marker YELLOW = MarkerManager.getMarker("yellow").addParents(COLORED);
    static final Marker GREEN = MarkerManager.getMarker("green").addParents(COLORED);
    static final Marker BLUE = MarkerManager.getMarker("blue").addParents(COLORED);

    private final ListAppender throttledListAppender = ListAppender.getListAppender("ThrottledListAppender");

    @Test
    void xmlConfigShouldBeParsedAsIntended() {
        assertThat(getLo4jConfiguration().getFilter())
                .isInstanceOfSatisfying(CompositeFilter.class, compositeFilter -> {
                    assertThat(compositeFilter.getFiltersArray()).anySatisfy(filter -> {
                        assertThat(filter).isInstanceOfSatisfying(RoutingFilter.class, routingFilter -> {
                            assertThat(routingFilter.filterRoutes()).allSatisfy(filterRoute -> {
                                assertThat(filterRoute.filterRouteIf().filter()).isInstanceOf(MarkerFilter.class);
                                assertThat(filterRoute.filterRouteThen().filter()).isInstanceOf(RegexFilter.class);
                            });
                        });
                    });
                });
    }

    @BeforeEach
    void beforeEach() {
        throttledListAppender.clear();
    }

    private static Configuration getLo4jConfiguration() {
        return LoggerContext.getContext(false).getConfiguration();
    }

    @Test
    void logsShouldBeFilteredBasedOnTheirColorMarker() {
        LOG.info(RED, "red lips");
        LOG.info(GREEN, "green grass");
        LOG.info(BLUE, "blue sky");
        LOG.info(YELLOW, "yellow submarine");


        assertThat(throttledListAppender.getEvents())
                .filteredOn(e -> e.getMarker().isInstanceOf(COLORED))
                .hasSize(4);

        throttledListAppender.clear();
        LOG.info(RED, "blue lips");
        LOG.info(GREEN, "brown grass");
        LOG.info(BLUE, "red sky");
        LOG.info(YELLOW, "blue submarine");
        assertThat(throttledListAppender.getEvents()).isEmpty();

        LOG.info("all is grey");
        LOG.info("black as midnight on a moonless night");
        assertThat(throttledListAppender.getEvents()).hasSize(2);

        throttledListAppender.clear();
        LOG.info("colors, give me colors");
        assertThat(throttledListAppender.getEvents()).isEmpty();
    }
}
