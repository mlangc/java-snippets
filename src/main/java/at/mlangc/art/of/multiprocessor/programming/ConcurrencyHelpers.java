package at.mlangc.art.of.multiprocessor.programming;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

public class ConcurrencyHelpers {
    public static void runSilently(InterruptibleRunnable op) {
        try {
            op.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    public static boolean supplySilently(InterruptibleBooleanSupplier supplier) {
        try {
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    public interface InterruptibleRunnable {
        void run() throws InterruptedException;
    }

    public interface InterruptibleBooleanSupplier {
        boolean get() throws InterruptedException;
    }
}
