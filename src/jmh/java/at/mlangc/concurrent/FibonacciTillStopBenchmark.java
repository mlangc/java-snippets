package at.mlangc.concurrent;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class FibonacciTillStopBenchmark {
    private final AtomicBoolean stop = new AtomicBoolean();

    @Param
    private MemoryOrdering memoryOrdering;

    @Param({"1", "10"})
    private int batchSize;

    @Param("100000")
    private int limit;

    @Benchmark
    public int fibTillStop() {
        final var mod = 1_000_000_007;
        var fib0 = 0;
        var fib1 = 1;

        for (int i = 0; !memoryOrdering.get(stop) && i < 100_000; i += batchSize) {
            for (int j = 0; j < batchSize; j++) {
                var fib2 = fib0 + fib1;
                if (fib2 >= mod) fib2 -= mod;
                fib0 = fib1;
                fib1 = fib2;
            }
        }

        return fib1;
    }

    @Setup
    public void setup() {
        if (limit % batchSize != 0) {
            throw new IllegalArgumentException("limit=" + limit + " doesn't divide batchSize=" + batchSize);
        }
    }
}
