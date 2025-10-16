package at.mlangc.log4j2.routing;


import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "FilterRouteIf", category = Node.CATEGORY, printObject = true)
public record FilterRouteIf(Filter filter) {
  @PluginFactory
  public static FilterRouteIf create(@PluginElement("FilterRouteIf") Filter filter) {
    return new FilterRouteIf(filter);
  }
}
