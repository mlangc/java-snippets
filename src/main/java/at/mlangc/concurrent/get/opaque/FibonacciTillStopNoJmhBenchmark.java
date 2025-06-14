package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

class FibonacciTillStopNoJmhBenchmark {
    private final AtomicBoolean stop = new AtomicBoolean();
    private final MemoryOrdering memoryOrdering;
    private long i = 0;
    private int fib0 = 0;
    private int fib1 = 1;
    private long runtimeNanos = 0;

    public static void main(String[] args) throws InterruptedException {
        var memoryOrdering = MemoryOrdering.VOLATILE;
        for (int i = 0; i < 3; i++) {
            benchmark(true, memoryOrdering);
        }

        out.println();
        var res = 0.0;
        var runs = 5;
        for (int i = 0; i < runs; i++) {
            res += benchmark(false, memoryOrdering);
        }

        out.println();
        out.printf("[%s] final result: %.5e%n", memoryOrdering.name().charAt(0), res / runs);
    }

    private static double benchmark(boolean warmup, MemoryOrdering memoryOrdering) throws InterruptedException {
        var benchmark = new FibonacciTillStopNoJmhBenchmark(memoryOrdering);
        var job = CompletableFuture.runAsync(benchmark::calcFibTillStopped);

        Thread.sleep(warmup ? 50 : 1000);
        memoryOrdering.set(benchmark.stop, true);
        job.join();

        var iterationsPerNs = (double) benchmark.i / benchmark.runtimeNanos;
        var prefix = warmup ? "[warmup] " : "";
        out.printf("%s[%s]: i=%.3e, rt=%.2ems, isPerNs=%.3e, fib1=%s%n",
                prefix, memoryOrdering.name().charAt(0), (double) benchmark.i, 1e-6 * benchmark.runtimeNanos,
                iterationsPerNs, benchmark.fib1);
        return iterationsPerNs;
    }

    FibonacciTillStopNoJmhBenchmark(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    private void calcFibTillStopped() {
        final var mod = 1_000_000_007;
        var t0 = System.nanoTime();
        while (!memoryOrdering.get(stop)) {
            var fib2 = fib0 + fib1;
            if (fib2 >= mod) fib2 -= mod;
            fib0 = fib1;
            fib1 = fib2;
            i++;
        }

        runtimeNanos += System.nanoTime() - t0;
    }
}
