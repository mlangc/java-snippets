package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

public class BroadcastProgressExample {
    final AtomicInteger progress = new AtomicInteger(0);

    void workerThread() {
        while (!done()) {
            work();
            progress.setOpaque(progress.getPlain() + 1);
        }
    }

    void monitoringThread() throws InterruptedException {
        while (!done()) {
            out.printf("progress=%s%n", progress.getOpaque());
            Thread.sleep(10);
        }
    }

    void work() {

    }

    boolean done() {
        return false;
    }
}
