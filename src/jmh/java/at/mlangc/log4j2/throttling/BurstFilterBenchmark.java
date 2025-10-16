package at.mlangc.log4j2.throttling;

import at.mlangc.log4j2.throttling.MarkerBasedBurstFilter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.BurstFilter;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, jvmArgsAppend = {"-Dlog4j2.configurationFile=log4j2-jmh.xml"})
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class BurstFilterBenchmark {
    static final int SEED = 313;
    public static final int NUM_SAMPLES = 100_000;
    static final Logger LOG = (Logger) LogManager.getLogger(BurstFilterBenchmark.class);
    static final Marker THROTTLING_MARKER = MarkerManager.getMarker("JMH_THROTTLING_MARKER");
    static final Marker OTHER_MARKER = MarkerManager.getMarker("JMH_OTHER_MARKER");

    final MarkerBasedBurstFilter markerBasedBurstFilter = new MarkerBasedBurstFilter(
            new MarkerBasedBurstFilter.MatchMarker(THROTTLING_MARKER.getName(), TimeUnit.SECONDS.toNanos(10), 10));

    final BurstFilter burstFilter = BurstFilter.newBuilder()
            .setLevel(Level.INFO)
            .setRate(1)
            .setMaxBurst(10)
            .build();

    @State(Scope.Thread)
    public static class MarkerBasedBurstFilterState {
        //@Param({"0", "0.5", "1"})
        @Param({"1"})
        double potentiallyThrottledRatio;

        Level[] levels = new Level[NUM_SAMPLES];
        Marker[] markers = new Marker[NUM_SAMPLES];
        int i;

        @Setup
        public void setup() {
            var rng = new Random(SEED);

            for (int i = 0; i < levels.length; i++) {
                var throttled = rng.nextDouble() < potentiallyThrottledRatio;
                markers[i] = throttled ? THROTTLING_MARKER : (rng.nextBoolean() ? OTHER_MARKER : null);
                levels[i] = rng.nextBoolean() ? Level.INFO : Level.WARN;
            }
        }

        Filter.Result nextFilter(MarkerBasedBurstFilter filter) {
            var res = filter.filter(LOG, levels[i], markers[i], "test");
            if (++i == NUM_SAMPLES) i = 0;
            return res;
        }
    }

    @State(Scope.Thread)
    public static class BurstFilterBenchmarkState {
        @Param({"0", "0.5", "1"})
        double potentiallyThrottledRatio;

        Level[] levels = new Level[NUM_SAMPLES];
        Marker[] markers = new Marker[NUM_SAMPLES];
        int i;

        @Setup
        public void setup() {
            var rng = new Random(SEED);

            for (int i = 0; i < levels.length; i++) {
                var throttled = rng.nextDouble() < potentiallyThrottledRatio;
                levels[i] = throttled ? Level.INFO : Level.WARN;
                markers[i] = rng.nextBoolean() ? OTHER_MARKER : null;
            }
        }

        Filter.Result nextFilter(BurstFilter filter) {
            var res = filter.filter(LOG, levels[i], markers[i], "test");
            if (++i == NUM_SAMPLES) i = 0;
            return res;
        }
    }

    @TearDown
    public void tearDown() {
        burstFilter.stop();
        markerBasedBurstFilter.stop();
    }

    @Benchmark
    public Filter.Result plain(BurstFilterBenchmarkState state) {
        return state.nextFilter(burstFilter);
    }

    @Benchmark
    public Filter.Result markerBased(MarkerBasedBurstFilterState state) {
        return state.nextFilter(markerBasedBurstFilter);
    }
}
