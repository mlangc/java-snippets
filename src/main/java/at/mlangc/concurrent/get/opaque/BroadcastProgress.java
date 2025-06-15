package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;

public class BroadcastProgress {
    private final AtomicLong progress = new AtomicLong();
    private final AtomicBoolean done = new AtomicBoolean();

    void startProgressPublishingOperation() {
        for (long step = 1; step <= 1_000_000_000L; step++) {
            doSomething();
            progress.setPlain(step);
        }

        done.setOpaque(true);
    }

    static void doSomething() {

    }

    void printProgress() {
        var lastProgress = progress.getPlain();
        var updateObserved = 0L;
        while (!done.getOpaque()) {
            var currentProgress = progress.getPlain();
            if (currentProgress != lastProgress && currentProgress % 10 == 0) {
                updateObserved++;
            }

            lastProgress = currentProgress;
        }

        out.printf("%s updates observed%n", updateObserved);
    }

    void run() {
        var progressPublishingJob = CompletableFuture.runAsync(this::startProgressPublishingOperation);
        printProgress();
        progressPublishingJob.join();
    }

    public static void main() {
        new BroadcastProgress().run();
    }
}
