package at.mlangc.log4j2.routing;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import java.util.Arrays;

import static java.util.Objects.requireNonNullElse;

@Plugin(name = "RoutingFilter", category = Node.CATEGORY, elementType = org.apache.logging.log4j.core.Filter.ELEMENT_TYPE, printObject = true)
public class RoutingFilter extends AbstractLifeCycle implements Filter {
    private final DefaultFilterRoute defaultFilterRoute;
    private final FilterRoute[] filterRoutes;

    public RoutingFilter(Result onNoRoute, DefaultFilterRoute defaultFilterRoute, FilterRoute[] filterRoutes) {
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
               "defaultFilterRoute=" + defaultFilterRoute +
               ", filterRoutes=" + Arrays.toString(filterRoutes) +
               '}';
    }

    public DefaultFilterRoute defaultFilterRoute() {
        return defaultFilterRoute;
    }

    public FilterRoute[] filterRoutes() {
        return filterRoutes;
    }

    @Override
    public Result getOnMismatch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result getOnMatch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return null;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, msg, p0))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, msg, p0);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, msg, p0);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, msg, t))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, msg, t);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, msg, t))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, msg, t);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg) {
        for (FilterRoute filterRoute : filterRoutes) {
            if (Result.ACCEPT.equals(filterRoute.filterRouteIf().filter().filter(logger, level, marker, msg))) {
                return filterRoute.filterRouteThen().filter().filter(logger, level, marker, msg);
            }
        }

        return defaultFilterRoute.filter().filter(logger, level, marker, msg);
    }

    @Override
    public Result filter(LogEvent event) {
        return null;
    }
}
