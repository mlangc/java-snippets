package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

class FibonacciTillStoppedBenchmark {
    private final AtomicBoolean stop = new AtomicBoolean();
    private final MemoryOrdering memoryOrdering;
    private long i = 1;
    private int fib0 = 0;
    private int fib1 = 1;

    public static void main(String[] args) throws InterruptedException {
        var memoryOrdering = MemoryOrdering.OPAQUE;
        for (int i = 0; i < 3; i++) {
            benchmark(true, memoryOrdering);
        }

        out.println();
        var res = 0L;
        var runs = 5;
        for (int i = 0; i < runs; i++) {
            res += benchmark(false, memoryOrdering);
        }

        out.println();
        out.printf("[%s] final result: %s%n", memoryOrdering.name().charAt(0), (double) res / runs);
    }

    private static long benchmark(boolean warmup, MemoryOrdering memoryOrdering) throws InterruptedException {
        var benchmark = new FibonacciTillStoppedBenchmark(memoryOrdering);
        var job = CompletableFuture.runAsync(benchmark::calcFibTillStopped);

        Thread.sleep(warmup ? 50 : 1000);
        memoryOrdering.set(benchmark.stop, true);
        job.join();

        var prefix = warmup ? "[warmup] " : "";
        out.printf("%s[%s]: i=%s, fib1=%s%n", prefix, memoryOrdering.name().charAt(0), benchmark.i, benchmark.fib1);
        return benchmark.i;
    }

    FibonacciTillStoppedBenchmark(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    private void calcFibTillStopped() {
        final var mod = 1_000_000_007;

        while (!memoryOrdering.get(stop)) {
            var fib2 = fib0 + fib1;
            if (fib2 >= mod) fib2 -= mod;
            fib0 = fib1;
            fib1 = fib2;
            i++;
        }
    }
}
