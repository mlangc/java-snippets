package at.mlangc.log4j2.routing;


import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "FilterRoute", category = Node.CATEGORY, printObject = true)
public record FilterRoute(FilterRouteIf filterRouteIf, FilterRouteThen filterRouteThen) {
  @PluginFactory
  public static FilterRoute create(
      @PluginElement("FilterRouteIf") FilterRouteIf filterRouteIf,
      @PluginElement("FilterRouteThen") FilterRouteThen filterRouteThen) {
    return new FilterRoute(filterRouteIf, filterRouteThen);
  }
}
