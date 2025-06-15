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
        var updatesObserved = 0L;
        var lastUpdateObserved = 0L;
        while (!done.getOpaque()) {
            var currentProgress = progress.getPlain();
            if (currentProgress != lastProgress && currentProgress % 10 == 0) {
                updatesObserved++;
                lastUpdateObserved = currentProgress;
            }

            lastProgress = currentProgress;
        }

        out.printf("updatesObserved=%s, lastUpdateObserved=%s%n", updatesObserved, lastUpdateObserved);
    }

    void run() {
        var progressPublishingJob = CompletableFuture.runAsync(this::startProgressPublishingOperation);
        printProgress();
        progressPublishingJob.join();
    }

    public static void main(String[] args) {
        new BroadcastProgress().run();
    }
}
