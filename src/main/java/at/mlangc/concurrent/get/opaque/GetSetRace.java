package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static java.lang.System.out;

class GetSetRace {
    public static void main() {
        final var memoryOrdering = MemoryOrdering.OPAQUE;
        final var iterations = 1_000_000;

        record Result(int r1, int r2) {}
        var results = new HashMap<Result, Integer>();

        var rng = ThreadLocalRandom.current();
        var state = new AtomicIntegerArray(128 * 1024 * 1024);
        var barrier = new CyclicBarrier(2);
        for (int i = 0; i < iterations; i++) {
            var xIdx = rng.nextInt(state.length());
            int yIdx = getYIdx(rng, state.length(), xIdx);
            state.setPlain(xIdx, 0);
            state.setPlain(yIdx, 0);

            var job1 = CompletableFuture.supplyAsync(() -> {
                withUncheckedExceptions(barrier::await);

                var r1 = memoryOrdering.get(state, yIdx);
                memoryOrdering.set(state, xIdx, r1);
                return r1;
            });

            var job2 = CompletableFuture.supplyAsync(() -> {
                withUncheckedExceptions(barrier::await);

                var r2 = memoryOrdering.get(state, xIdx);
                memoryOrdering.set(state, yIdx, 42);
                return r2;
            });

            results.merge(new Result(job1.join(), job2.join()), 1, Integer::sum);
        }

        out.printf("Results: %s%n", results);
    }

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
