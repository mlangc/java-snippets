package at.mlangc.log4j2.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.junit.jupiter.api.Test;

class RoutingFilterTest {
  private static final Logger LOG = LogManager.getLogger(RoutingFilterTest.class);

  @Test
  void xmlConfigShouldBeParsedProperly() {
    assertThat(LoggerContext.getContext().getConfiguration().getFilter())
        .isInstanceOfSatisfying(CompositeFilter.class, compositeFilter -> {
          assertThat(compositeFilter.getFiltersArray()).anySatisfy(filter -> {
            assertThat(filter).isInstanceOfSatisfying(RoutingFilter.class, routingFilter -> {
              assertThat(routingFilter.defaultFilterRoute().filter()).isInstanceOf(RegexFilter.class);
              assertThat(routingFilter.filterRoutes()).allSatisfy(filterRoute -> {
                assertThat(filterRoute.filterRouteIf().filter()).isInstanceOf(MarkerFilter.class);
                assertThat(filterRoute.filterRouteThen().filter()).isInstanceOf(RegexFilter.class);
              });
            });
          });
        });
  }
}
