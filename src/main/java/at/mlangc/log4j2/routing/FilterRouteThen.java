package at.mlangc.log4j2.routing;


import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "FilterRouteThen", category = Node.CATEGORY, printObject = true)
public record FilterRouteThen(Filter filter) {
  @PluginFactory
  public static FilterRouteThen create(@PluginElement("FilterRouteThen") Filter filter) {
    return new FilterRouteThen(filter);
  }
}
