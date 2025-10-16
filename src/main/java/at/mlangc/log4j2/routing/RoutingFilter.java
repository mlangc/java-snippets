package at.mlangc.log4j2.routing;


import java.util.Arrays;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

@Plugin(name = "RoutingFilter", category = Node.CATEGORY, elementType = org.apache.logging.log4j.core.Filter.ELEMENT_TYPE, printObject = true)
public class RoutingFilter extends AbstractFilter {
  private final DefaultFilterRoute defaultFilterRoute;
  private final FilterRoute[] filterRoutes;

  public RoutingFilter(DefaultFilterRoute defaultFilterRoute, FilterRoute[] filterRoutes) {
    this.defaultFilterRoute = defaultFilterRoute;
    this.filterRoutes = filterRoutes;
  }

  @PluginFactory
  public static RoutingFilter create(
      @PluginElement("DefaultFilterRoute") DefaultFilterRoute defaultFilterRoute,
      @PluginElement("FilterRoute") FilterRoute... filterRoutes
  ) {
    return new RoutingFilter(defaultFilterRoute, filterRoutes);
  }

  @Override
  public String toString() {
    return "RoutingFilter{" +
        "defaultFilterRoute=" + defaultFilterRoute +
        ", filterRoutes=" + Arrays.toString(filterRoutes) +
        ", onMatch=" + onMatch +
        ", onMismatch=" + onMismatch +
        '}';
  }

  public DefaultFilterRoute defaultFilterRoute() {
    return defaultFilterRoute;
  }

  public FilterRoute[] filterRoutes() {
    return filterRoutes;
  }
}
