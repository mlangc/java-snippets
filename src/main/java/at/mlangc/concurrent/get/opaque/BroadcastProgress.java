package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

public class BroadcastProgress {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger progress = new AtomicInteger();
    private final AtomicBoolean done = new AtomicBoolean();

    BroadcastProgress(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    void startPublishingProgress() {
        while (!done.getOpaque()) {
            publishProgress();
        }
    }

    void publishProgress() {
        for (int step = 0; step < 100_000L; step++) {
            doSomething();
            memoryOrdering.set(progress, step);
        }
    }

    static void doSomething() {

    }

    void printProgress() {
        var observationCounts = new long[10];
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
        var printingJob = CompletableFuture.runAsync(this::printProgress);
        Thread.sleep(100);

        done.setOpaque(true);
        publishingJob.join();
        printingJob.join();
    }

    public static void main() throws InterruptedException {
        new BroadcastProgress(MemoryOrdering.PLAIN).run();
    }
}
