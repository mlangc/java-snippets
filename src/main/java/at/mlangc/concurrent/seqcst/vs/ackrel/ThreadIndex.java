package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;

class ThreadIndex {
    private static final String PREFIX = "indexed-thread-";

    static int current() {
        Thread currentThread = Thread.currentThread();
        checkArgument(currentThread.getName().startsWith(PREFIX), "Unexpected thread name '%s'", currentThread.getName());

        try {
            return Integer.parseInt(currentThread.getName().substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot extract thread index from '" + currentThread.getName() + "'", e);
        }
    }

    static ExecutorService newIndexedCachedThreadPool() {
        return Executors.newCachedThreadPool(Thread.ofPlatform().daemon().name(PREFIX, 0).factory());
    }

    static String toName(int index) {
        return PREFIX + index;
    }
}
