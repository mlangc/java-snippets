package at.mlangc.log4j2.routing;


import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.RegexFilter;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

@Plugin(name = "RoutingFilter", category = Node.CATEGORY, elementType = org.apache.logging.log4j.core.Filter.ELEMENT_TYPE, printObject = true)
public class RoutingFilter extends AbstractFilter {
    private final Result onNoRoute;
    private final DefaultFilterRoute defaultFilterRoute;
    private final FilterRoute[] filterRoutes;

    public RoutingFilter(Result onNoRoute, DefaultFilterRoute defaultFilterRoute, FilterRoute[] filterRoutes) {
        this.onNoRoute = onNoRoute;
        this.defaultFilterRoute = defaultFilterRoute;
        this.filterRoutes = filterRoutes;
    }

    @PluginFactory
    public static RoutingFilter create(
            @PluginAttribute("onNoRoute") Result onNoRoute,
            @PluginElement("DefaultFilterRoute") DefaultFilterRoute defaultFilterRoute,
            @PluginElement("FilterRoute") FilterRoute... filterRoutes
    ) {
        return new RoutingFilter(requireNonNullElse(onNoRoute, Result.NEUTRAL), defaultFilterRoute, filterRoutes);
    }

    @Override
    public String toString() {
        return "RoutingFilter{" +
               "onNoRoute=" + onNoRoute +
               ", defaultFilterRoute=" + defaultFilterRoute +
               ", filterRoutes=" + Arrays.toString(filterRoutes) +
               '}';
    }

    public DefaultFilterRoute defaultFilterRoute() {
        return defaultFilterRoute;
    }

    public FilterRoute[] filterRoutes() {
        return filterRoutes;
    }
}
