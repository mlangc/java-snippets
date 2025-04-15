package at.mlangc.art.of.multiprocessor.programming;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

public class TestHelpers {
    public static void withUncheckedInterrupts(InterruptibleRunnable op) {
        try {
            op.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    public interface InterruptibleRunnable {
        void run() throws InterruptedException;
    }
}
