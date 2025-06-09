package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.out;
import static java.util.concurrent.CompletableFuture.delayedExecutor;

class GetSetRace implements AutoCloseable {
    private final MemoryOrdering memoryOrdering;
    private final LongAdder failedAttempts = new LongAdder();
    private final ExecutorService virtualThreadsExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService racingTasksExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean futureWriteObserved = new AtomicBoolean();
    private final AtomicIntegerArray workspace;
    private final int parallelism;
    private final int indicesPerSingleRace;

    public static void main(String[] args) throws InterruptedException {
        try (var race = new GetSetRace(100_000_000, 100, MemoryOrdering.OPAQUE)) {
            race.run();
        }
    }

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
        private final int id;
        private int ix;
        private int iy;

        SingleRace(int id) {
            this.id = id;
        }

        private void assignIxIy(ThreadLocalRandom rng) {
            var tmpIx = rng.nextInt(indicesPerSingleRace);

            var tmpIy = rng.nextInt(indicesPerSingleRace);
            while (tmpIy == tmpIx) {
                tmpIy = rng.nextInt(indicesPerSingleRace);
            }

            this.ix = id + tmpIx * parallelism;
            this.iy = id + tmpIy * parallelism;
        }

        int getX() {
            return memoryOrdering.get(workspace, ix);
        }

        void setX(int value) {
            memoryOrdering.set(workspace, ix, value);
        }

        void resetXY() {
            workspace.setPlain(ix, 0);
            workspace.setPlain(iy, 0);
        }

        int getY() {
            return memoryOrdering.get(workspace, iy);
        }

        void setY(int value) {
            memoryOrdering.set(workspace, iy, value);
        }

        void tryObserveFutureWrite(int maxTries) {
            var rng = ThreadLocalRandom.current();
            for (int tries = 1; tries <= maxTries && !done.getOpaque(); tries++) {
                if (tryObserveFutureWrite(rng)) {
                    futureWriteObserved.setOpaque(true);
                    done.setOpaque(true);
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
                var r2 = getX();
                setY(42);
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

    GetSetRace(int workspaceSize, int parallelism, MemoryOrdering memoryOrdering) {
        checkArgument(workspaceSize % parallelism == 0);

        this.memoryOrdering = memoryOrdering;
        this.workspace = new AtomicIntegerArray(workspaceSize);
        this.parallelism = parallelism;
        this.indicesPerSingleRace = workspaceSize / parallelism;

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

    void run() {
        var triesPerTask = 100_000_000;

        CompletableFuture.allOf(
                IntStream.range(0, parallelism)
                        .mapToObj(id -> CompletableFuture.runAsync(() -> new SingleRace(id).tryObserveFutureWrite(triesPerTask)))
                        .toArray(CompletableFuture<?>[]::new)
        ).join();

        if (futureWriteObserved.getOpaque()) {
            out.printf("Observed future write after approximately %s attempts%n", failedAttempts.longValue());
        } else {
            out.printf("Did not observe future write after %s attempts%n",failedAttempts.longValue());
        }
    }
}
