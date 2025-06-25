package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

class FibonacciTillStopNoJmhBenchmark {
    public static final int MOD = 0xbabe;
    private final AtomicBoolean stop = new AtomicBoolean();
    private final MemoryOrdering memoryOrdering;
    private long i = 0;
    private int fib0 = 0;
    private int fib1 = 1;
    private long runtimeNanos = 0;
    private int[] fibs = new int[1024];

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
        var job = CompletableFuture.runAsync(benchmark::calcFibArrayTillStopped);

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

        this.fibs[1] = 1;
        for (int j = 2; j < fibs.length; j++) {
            this.fibs[j] = this.fibs[j - 1] + this.fibs[j - 2];
            if (this.fibs[j] >= MOD) this.fibs[j] -= MOD;
        }
    }

    private void calcFibTillStopped() {
        var t0 = System.nanoTime();
        while (!memoryOrdering.get(stop)) {
            var fib2 = fib0 + fib1;
            if (fib2 >= MOD) fib2 -= MOD;
            fib0 = fib1;
            fib1 = fib2;
            i++;
        }

        runtimeNanos += System.nanoTime() - t0;
    }

    private void calcFibArrayTillStopped() {
        i = fibs.length;
        var t0 = System.nanoTime();
        while (!memoryOrdering.get(stop)) {
            var fibn = fibs[fibs.length - 1] + fibs[fibs.length - 2];
            if (fibn >= MOD) fibn -= MOD;

            for (int j = 0; j < fibs.length - 1; j++) {
                fibs[j] = fibs[j + 1];
            }

            fibs[fibs.length - 1] = fibn;
            i++;
        }

        fib0 = fibs[fibs.length - 2];
        fib1 = fibs[fibs.length - 1];
        runtimeNanos += System.nanoTime() - t0;
    }
}
