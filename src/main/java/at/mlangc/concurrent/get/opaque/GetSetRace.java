package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;

class GetSetRace {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger x = new AtomicInteger();
    private final AtomicInteger y = new AtomicInteger();
    private final AtomicBoolean stop = new AtomicBoolean();

    private final AtomicLong barrier1 = new AtomicLong(0);
    private final AtomicLong barrier2 = new AtomicLong(0);

    GetSetRace(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    CompletableFuture<Void> startRacer1() {
        return CompletableFuture.runAsync(() -> {
            while (!stop.getOpaque()) {
                awaitBarrier(barrier1, barrier2);
                var r1 = memoryOrdering.get(y);
                memoryOrdering.set(x, r1);

                awaitBarrier(barrier1, barrier2);
                x.set(0);
            }
        });
    }

    CompletableFuture<Void> startRacer2() {
        return CompletableFuture.runAsync(() -> {
            while (!stop.getOpaque()) {
                awaitBarrier(barrier2, barrier1);
                var r2 = memoryOrdering.get(x);
                memoryOrdering.set(y, 42);

                if (r2 == 42) {
                    out.println("Found");
                    stop.setOpaque(true);
                }

                awaitBarrier(barrier2, barrier1);
                y.set(0);
            }
        });
    }


    public static void main() throws InterruptedException, ExecutionException, TimeoutException {
        var race = new GetSetRace(MemoryOrdering.OPAQUE);
        var racer1 = race.startRacer1();
        var racer2 = race.startRacer2();

        Thread.sleep(10_000);
        race.stop.setOpaque(true);

        racer1.get(1, TimeUnit.SECONDS);
        racer2.get(1, TimeUnit.SECONDS);
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

    private void awaitBarrier(AtomicLong mine, AtomicLong other) {
        mine.setOpaque(mine.getPlain() + 1);

        while (!stop.getOpaque() && other.getOpaque() != mine.getPlain()) {
            Thread.onSpinWait();
        }
    }

    record Result(int r1, int r2) {}

    private static int getYIdx(Random random, int xIdx, int len) {
        while (true) {
            var yIdx = random.nextInt(len);
            if (yIdx != xIdx) return yIdx;
        }
    }
}
