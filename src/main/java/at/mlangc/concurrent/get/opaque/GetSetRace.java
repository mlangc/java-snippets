package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.out;
import static java.util.concurrent.CompletableFuture.delayedExecutor;

class GetSetRace implements AutoCloseable {
    private final MemoryOrdering memoryOrdering;
    private final LongAdder failedAttempts = new LongAdder();
    private final ExecutorService virtualThreadsExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService racingTasksExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean futureWriteObserved = new AtomicBoolean();

    @Override
    public void close() throws InterruptedException {
        virtualThreadsExecutor.shutdown();
        racingTasksExecutor.shutdown();

        if (!virtualThreadsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new AssertionError("Termination timed out");
        }

        if (!racingTasksExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new AssertionError("Termination timed out");
        }
    }

    class SingleRace {
        private final AtomicIntegerArray ints = new AtomicIntegerArray(10 * 1024 * 1024);
        private int ix;
        private int iy;

        private void assignIxIy(ThreadLocalRandom rng) {
            this.ix = rng.nextInt(ints.length());

            var tmpIy = rng.nextInt(ints.length());
            while (tmpIy == ix) {
                tmpIy = rng.nextInt(ints.length());
            }

            this.iy = tmpIy;
        }

        int getX() {
            return memoryOrdering.get(ints, ix);
        }

        void setX(int value) {
            memoryOrdering.set(ints, ix, value);
        }

        void resetXY() {
            ints.setPlain(ix, 0);
            ints.setPlain(iy, 0);
        }

        int getY() {
            return memoryOrdering.get(ints, iy);
        }

        void setY(int value) {
            memoryOrdering.set(ints, iy, value);
        }

        void tryObserveFutureWrite(int maxTries) {
            var rng = ThreadLocalRandom.current();
            for (int tries = 1; tries <= maxTries && !done.getOpaque(); tries++) {
                if (tryObserveFutureWrite(rng)) {
                    futureWriteObserved.setOpaque(true);
                    return;
                } else {
                    failedAttempts.increment();
                }
            }
        }

        boolean tryObserveFutureWrite(ThreadLocalRandom rng) {
            Runnable racer1 = () -> {
                var r1 = getY();
                setX(r1);
            };

            Supplier<Boolean> racer2 = () -> {
                setY(42);
                var r2 = getX();
                return r2 == 42;
            };

            assignIxIy(rng);
            var job1 = CompletableFuture.runAsync(racer1, randomlyDelayedExecutor(rng));
            var job2 = CompletableFuture.supplyAsync(racer2, randomlyDelayedExecutor(rng));

            job1.join();
            var res = job2.join();
            resetXY();
            return res;
        }

        private Executor randomlyDelayedExecutor(ThreadLocalRandom rng) {
            return delayedExecutor(rng.nextInt(10_000), TimeUnit.NANOSECONDS, racingTasksExecutor);
        }
    }


    public static void main(String[] args) throws InterruptedException {
        try (var race = new GetSetRace(MemoryOrdering.OPAQUE)) {
            var triesPerTask = 100_000_000;
            var numTasks = 100;

            CompletableFuture.allOf(
                    IntStream.range(0, numTasks)
                            .mapToObj(ignore -> CompletableFuture.runAsync(() -> race.new SingleRace().tryObserveFutureWrite(triesPerTask)))
                            .toArray(CompletableFuture<?>[]::new)
            ).join();

            if (race.futureWriteObserved.getOpaque()) {
                out.printf("Observed future write after approximately %s attempts%n", race.failedAttempts.longValue());
            } else {
                out.printf("Did not observe future write after %s attempts%n", race.failedAttempts.longValue());
            }
        }
    }

    GetSetRace(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;

        Thread.ofVirtual().start(() -> {
            var lastFailedAttempts = failedAttempts.longValue();
            while (!done.getOpaque()) {
                try {
                    Thread.sleep(5000);
                    var currentFailedAttempts = failedAttempts.longValue();
                    if (lastFailedAttempts != currentFailedAttempts) {
                        out.printf("%s failed attempts so far%n", currentFailedAttempts);
                        lastFailedAttempts = currentFailedAttempts;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new UncheckedInterruptedException(e);
                }
            }
        });
    }

}
