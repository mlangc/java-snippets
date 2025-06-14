package at.mlangc.concurrent;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class FibonacciTillStopBenchmark {
    private final AtomicBoolean stop = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Param
    private MemoryOrdering memoryOrdering;
    private Future<?> schedule;

    @Setup
    public void setup() {
        schedule = scheduler.scheduleWithFixedDelay(() ->
                memoryOrdering.set(stop, true), 0, 50, TimeUnit.MILLISECONDS);
    }

    @TearDown
    public void tearDown() {
        schedule.cancel(false);
        scheduler.shutdown();
    }

    @Benchmark
    public int fibTillStop() {
        final var mod = 1_000_000_007;
        var fib0 = 0;
        var fib1 = 1;
        var i = 0;
        stop.setPlain(false);

        while (!memoryOrdering.get(stop) && i < 1000) {
            var fib2 = fib0 + fib1;
            if (fib2 >= mod) fib2 -= mod;
            fib0 = fib1;
            fib1 = fib2;
            i++;
        }

        return fib1;
    }
}
