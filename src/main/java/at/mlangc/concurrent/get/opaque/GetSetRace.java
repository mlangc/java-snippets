package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;

class GetSetRace {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger[] atomicInts = new AtomicInteger[16 * 1024 * 1024];
    private final AtomicInteger x;
    private final AtomicInteger y;
    private final AtomicBoolean stop = new AtomicBoolean();

    private final AtomicLong barrier1 = new AtomicLong(0);
    private final AtomicLong barrier2 = new AtomicLong(0);

    private boolean observedFutureWrite;

    GetSetRace(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
        Arrays.setAll(atomicInts, ignore -> new AtomicInteger());
        this.x = atomicInts[0];
        this.y = atomicInts[atomicInts.length - 1];
    }

    CompletableFuture<Long> startRacer1() {
        return CompletableFuture.supplyAsync(() -> {
            var iterations = 0L;
            while (!stop.getOpaque()) {
                awaitBarrier(barrier1, barrier2);
                var r1 = memoryOrdering.get(y);
                memoryOrdering.set(x, r1);
                iterations++;

                awaitBarrier(barrier1, barrier2);
                x.setOpaque(0);
            }

            return iterations;
        });
    }

    CompletableFuture<Long> startRacer2() {
        return CompletableFuture.supplyAsync(() -> {
            var iterations = 0L;
            while (!stop.getOpaque()) {
                awaitBarrier(barrier2, barrier1);
                var r2 = memoryOrdering.get(x);
                memoryOrdering.set(y, 42);

                if (r2 == 42) {
                    observedFutureWrite = true;
                    stop.setOpaque(true);
                }
                iterations++;

                awaitBarrier(barrier2, barrier1);
                y.setOpaque(0);
            }

            return iterations;
        });
    }


    public static void main() throws InterruptedException, ExecutionException, TimeoutException {
        var race = new GetSetRace(MemoryOrdering.OPAQUE);
        var racer1 = race.startRacer1();
        var racer2 = race.startRacer2();

        Thread.sleep(10_000);
        race.stop.setOpaque(true);

        var iterations1 = racer1.get(1, TimeUnit.SECONDS);
        var iterations2 = racer2.get(1, TimeUnit.SECONDS);
        var prefix = String.format("[iterations=(%s, %s)]", iterations1, iterations2);

        if (race.observedFutureWrite) {
            out.printf("%s Observed effects of future write", prefix);
        } else {
            out.printf("%s Effects of future write where not observed", prefix);
        }
    }

    private void awaitBarrier(AtomicLong mine, AtomicLong other) {
        mine.setOpaque(mine.getPlain() + 1);

        while (!stop.getOpaque() && other.getOpaque() < mine.getPlain()) {
            Thread.onSpinWait();
        }
    }
}
