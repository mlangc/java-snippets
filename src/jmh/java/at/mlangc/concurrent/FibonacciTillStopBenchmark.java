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

    @Benchmark
    public int fibTillStop() {
        return fibTillStop(memoryOrdering, stop);
    }

    private static int fibTillStop(MemoryOrdering memoryOrdering, AtomicBoolean stop) {
        final var mod = 1_000_000_007;
        var fib0 = 0;
        var fib1 = 1;
        stop.setPlain(false);

        for (int i = 0; !memoryOrdering.get(stop) && i < 100_000; i++) {
            var fib2 = fib0 + fib1;
            if (fib2 >= mod) fib2 -= mod;
            fib0 = fib1;
            fib1 = fib2;

        }

        return fib1;
    }
}
