package at.mlangc.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(name = "MarkerBasedBurstFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class MarkerBasedBurstFilter extends AbstractFilter {
    @Plugin(name = "MatchMarker", category = Node.CATEGORY, printObject = true)
    public record MatchMarker(String marker, long intervalNanos, int maxLogs) {
        @PluginFactory
        public static MatchMarker createNested(
                @Required @PluginAttribute("marker") String marker,
                @Required @PluginAttribute("interval") int interval,
                @Required @PluginAttribute("maxLogs") int maxLogs,
                @Required @PluginAttribute("timeUnit") TimeUnit timeUnit) {
            return new MatchMarker(marker, timeUnit.toNanos(interval), maxLogs);
        }
    }

    private static class PerMarkerState {
        final String marker;
        final int maxLogs;
        final AtomicLong permittedLogs;
        final Future<?> schedule;

        PerMarkerState(String marker, long intervalNanos, int maxLogs) {
            this.marker = marker;
            this.maxLogs = maxLogs;
            this.permittedLogs = new AtomicLong(maxLogs);

            this.schedule = ForkJoinPool.commonPool().scheduleAtFixedRate(
                    () -> permittedLogs.set(maxLogs),
                    ThreadLocalRandom.current().nextLong(0, intervalNanos),
                    intervalNanos,
                    TimeUnit.NANOSECONDS);
        }

        boolean acquirePermit() {
            return permittedLogs.get() > 0 && permittedLogs.decrementAndGet() >= 0;
        }
    }

    private final PerMarkerState[] perMarkerStates;

    MarkerBasedBurstFilter(MatchMarker... matchMarkers) {
        this.perMarkerStates = new PerMarkerState[matchMarkers.length];

        var markersSeen = new HashSet<String>();
        for (int i = 0; i < matchMarkers.length; i++) {
            MatchMarker matchMarker = matchMarkers[i];

            if (!markersSeen.add(matchMarker.marker)) {
                throw new IllegalArgumentException("Marker configured more than once: " + matchMarker.marker);
            }

            var perMarkerState = new PerMarkerState(matchMarker.marker, matchMarker.intervalNanos, matchMarker.maxLogs);
            this.perMarkerStates[i] = perMarkerState;
        }
    }

    @PluginFactory
    public static MarkerBasedBurstFilter createFilter(@PluginElement("MatchMarker") MatchMarker[] matchMarkers) {
        return new MarkerBasedBurstFilter(matchMarkers);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return filter(marker);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter(marker);
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getMarker());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg) {
        return filter(marker);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(marker);
    }

    private Result filter(Marker marker) {
        if (marker == null) return Result.NEUTRAL;

        Result res = Result.NEUTRAL;
        for (var perMarkerState : perMarkerStates) {
            if (marker.isInstanceOf(perMarkerState.marker) && !perMarkerState.acquirePermit()) {
                res = Result.DENY;
            }
        }

        return res;
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        setStopping();

        for (PerMarkerState state : perMarkerStates) {
            state.schedule.cancel(false);
        }

        setStopped();
        return true;
    }
}
