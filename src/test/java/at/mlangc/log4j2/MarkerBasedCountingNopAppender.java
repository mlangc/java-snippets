package at.mlangc.log4j2;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(name = "MarkerBasedCountingNopAppender", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = false)
public class MarkerBasedCountingNopAppender extends AbstractAppender {
    public static final Marker NO_MARKER = MarkerManager.getMarker("NULL:" + UUID.randomUUID());

    private final ConcurrentHashMap<Marker, AtomicLong> counters = new ConcurrentHashMap<>();

    protected MarkerBasedCountingNopAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, false, null);
    }

    @Override
    public void append(LogEvent event) {
        var marker = event.getMarker() == null ? NO_MARKER : event.getMarker();
        counters.computeIfAbsent(marker, ignore -> new AtomicLong()).incrementAndGet();
    }

    @PluginFactory
    public static MarkerBasedCountingNopAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new MarkerBasedCountingNopAppender(name, filter, layout);
    }

    public Map<Marker, Long> currentCounts() {
        var res = HashMap.<Marker, Long>newHashMap(counters.size());
        counters.forEach((m, c) -> res.put(m, c.get()));
        return res;
    }

    public static MarkerBasedCountingNopAppender getFromConfiguration(final String name) {
        return LoggerContext.getContext(false).getConfiguration().getAppender(name);
    }

    public void resetCounters() {
        counters.clear();
    }
}
