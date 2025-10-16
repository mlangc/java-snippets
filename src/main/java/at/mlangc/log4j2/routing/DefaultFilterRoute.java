package at.mlangc.log4j2.routing;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "DefaultFilterRoute", category = Node.CATEGORY)
public record DefaultFilterRoute(Filter filter) {
  @PluginFactory
  public static DefaultFilterRoute create(@PluginElement("DefaultFilterRoute") Filter filter) {
    return new DefaultFilterRoute(filter);
  }
}
