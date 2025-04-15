package at.mlangc.art.of.multiprocessor.programming.ch7;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class ExponentialBackoff {
    private final long capNanos;
    private long currentNanos;

    public ExponentialBackoff(Duration initial, Duration cap) {
        Preconditions.checkArgument(initial.isPositive());
        Preconditions.checkArgument(initial.compareTo(cap) <= 0);

        this.currentNanos = initial.toNanos();
        this.capNanos = cap.toNanos();
    }

    public void backoff() {
        var random = ThreadLocalRandom.current();
        var sleepNanos = Math.min(capNanos, random.nextLong(currentNanos / 2, currentNanos + currentNanos / 2));
        sleep(sleepNanos);
        currentNanos = Math.min(currentNanos * 2, capNanos);
    }

    private static void sleep(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }
}
