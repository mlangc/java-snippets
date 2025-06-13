package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

class BroadcastStopExample {
    private final AtomicBoolean stop = new AtomicBoolean();

    void workerThreads() {
        while (!stop.getOpaque()) {
            doSomeWork();
        }

        out.println("Stopped");
    }

    void controllerThread() {
        stop.setOpaque(true);
    }

    void doSomeWork() {

    }
}
