package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

class SpinTillStop {
    private final MemoryOrdering memoryOrdering;
    private final AtomicBoolean stop = new AtomicBoolean();

    public static void main(String[] args) throws InterruptedException {
        new SpinTillStop(MemoryOrdering.PLAIN).run();
    }

    SpinTillStop(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    void run() throws InterruptedException {
        var spinner = Thread.ofPlatform().start(() -> {
            var spins = 0L;
            while (!memoryOrdering.get(stop)) {
                spins += 1;
            }

            out.printf("Done after %s spins%n", spins);
        });

        Thread.sleep(500);
        memoryOrdering.set(stop, true);
        out.printf("Stop signal sent%n");

        spinner.join();
    }
}
