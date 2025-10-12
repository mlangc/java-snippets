package at.mlangc.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(value = 1, jvmArgsAppend = {"-Dlog4j2.configurationFile=log4j2-jmh.xml"})
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class BurstFilterBenchmark {
    private static final Logger LOG = LogManager.getLogger(BurstFilterBenchmark.class);
    private static final Logger LOG_PLAIN_BURST_FILTER = LogManager.getLogger(BurstFilterBenchmark.class.getCanonicalName() + ".PlainBurstFilter");
    private static final Logger LOG_MARKER_BASED_BURST_FILTER = LogManager.getLogger(BurstFilterBenchmark.class.getCanonicalName() + ".MarkerBasedBurstFilter");
    private static final Marker AT_MOST_10_LOGS_IN_10_SECS = MarkerManager.getMarker("atMost10LogsIn10Secs");

    @Benchmark
    public void logBaseline() {
        LOG.info("test");
    }

    @Benchmark
    public void logPlainBurstFilter() {
        LOG_PLAIN_BURST_FILTER.info("test");
    }

    @Benchmark
    public void logMarkerBasedBurstFilter() {
        LOG_MARKER_BASED_BURST_FILTER.info(AT_MOST_10_LOGS_IN_10_SECS, "test");
    }
}
