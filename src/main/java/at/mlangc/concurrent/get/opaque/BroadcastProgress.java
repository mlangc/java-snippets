package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

public class BroadcastProgress {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger progress = new AtomicInteger();
    private final AtomicBoolean done = new AtomicBoolean();

    private int progress2 = 0;

    private static final VarHandle PROGRESS;

    static {
        try {
            PROGRESS = MethodHandles.lookup().findVarHandle(BroadcastProgress.class, "progress2", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    BroadcastProgress(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    void startPublishingProgress() {
        while (!done.getOpaque()) {
            publishProgress();
        }
    }

    void publishProgress() {
        for (int step = 0; step < 100_000; step++) {
            doSomething();
            PROGRESS.setOpaque(this, step);
        }
    }

    static void doSomething() {

    }

    void observeProgress() {
        var observationCounts = new long[4];
        while (!done.getOpaque()) {
            var currentProgress = memoryOrdering.get(progress);
            observationCounts[(currentProgress % observationCounts.length)]++;
        }

        var max = Arrays.stream(observationCounts).max().orElseThrow();
        out.println("Updates observed: ");
        for (int i = 0, observationCountsLength = observationCounts.length; i < observationCountsLength; i++) {
            long observationCount = observationCounts[i];
            out.printf("%2d|%s%n", i, "#".repeat(Math.round((10f * observationCount) / max)));
        }
    }

    void run() throws InterruptedException {
        var publishingJob = CompletableFuture.runAsync(this::startPublishingProgress);
        var observingJob = CompletableFuture.runAsync(this::observeProgress);
        Thread.sleep(100);

        done.setOpaque(true);
        publishingJob.join();
        observingJob.join();
    }

    public static void main() throws InterruptedException {
        new BroadcastProgress(MemoryOrdering.OPAQUE).run();
    }
}
