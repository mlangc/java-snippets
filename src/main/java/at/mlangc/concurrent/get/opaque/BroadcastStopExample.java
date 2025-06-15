package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.CompletableFuture;
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

    static long brokenBroadcast() throws InterruptedException {
        var stop = new AtomicBoolean();
        var job = CompletableFuture.supplyAsync(() -> {
            var spins = 0L;
            while (!stop.getPlain()) {
                spins++;
            }

            return spins;
        });

        Thread.sleep(100);
        stop.setPlain(true);
        return job.join();
    }

    static long brokenButAccidentallyWorkingBroadcast() throws InterruptedException {
        var stop = new AtomicBoolean();
        var job = CompletableFuture.supplyAsync(() -> {
            var spins = 0L;
            while (!stop.getPlain()) {
                if (++spins % 1_000_000_000 == 0) {
                    out.printf("%s spins already...%n", spins);
                }
            }

            return spins;
        });

        Thread.sleep(100);
        stop.setPlain(true);
        return job.join();
    }

    public static void main() throws InterruptedException {
        var spins = brokenButAccidentallyWorkingBroadcast();
        out.printf("Done after %s spins%n", spins);
    }
}
