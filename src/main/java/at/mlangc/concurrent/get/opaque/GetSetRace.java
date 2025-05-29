package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.System.out;

class GetSetRace {
    public static void main() {
        final var memoryOrdering = MemoryOrdering.OPAQUE;
        final var iterations = 4_000_000;
        final var parallelRuns = 4;
        final var iterationsPerRun = iterations / parallelRuns;

        var runs = IntStream.range(0, parallelRuns)
                .mapToObj(ignore -> CompletableFuture.supplyAsync(() -> run(iterationsPerRun, memoryOrdering)))
                .toList();

        var res = new HashMap<Result, Integer>();
        for (var run : runs) {
            run.join().forEach((r, c) -> res.merge(r, c, Integer::sum));
        }

        out.printf("Results: %s%n", res);
    }

    private static HashMap<Result, Integer> run(int iterations, MemoryOrdering memoryOrdering) {
        var results = new HashMap<Result, Integer>();

        var rng = ThreadLocalRandom.current();
        var state = new AtomicInteger[1024];
        Arrays.setAll(state, ignore -> new AtomicInteger());
        for (int i = 0; i < iterations; i++) {
            var xIdx = rng.nextInt(state.length);
            int yIdx = getYIdx(rng, xIdx, state.length);
            var x = state[xIdx];
            var y = state[yIdx];
            x.setPlain(0);
            y.setPlain(0);

            var job1 = CompletableFuture.supplyAsync(() -> {
                var r1 = memoryOrdering.get(y);
                memoryOrdering.set(x, r1);
                return r1;
            });

            var job2 = CompletableFuture.supplyAsync(() -> {
                var r2 = memoryOrdering.get(x);
                memoryOrdering.set(y, 42);
                return r2;
            });

            results.merge(new Result(job1.join(), job2.join()), 1, Integer::sum);
        }

        return results;
    }

    record Result(int r1, int r2) {}

    private static int getYIdx(Random random, int xIdx, int len) {
        while (true) {
            var yIdx = random.nextInt(len);
            if (yIdx != xIdx) return yIdx;
        }
    }

    interface RunnableWithCheckedExceptions {
        void run() throws Exception;
    }

    static void withUncheckedExceptions(RunnableWithCheckedExceptions op) {
        try {
            op.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
