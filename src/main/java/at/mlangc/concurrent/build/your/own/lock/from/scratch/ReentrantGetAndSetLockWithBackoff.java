package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;

public class ReentrantGetAndSetLockWithBackoff implements SimpleLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private int entries;

    private final long initialBackoffLimitNanos;
    private final long finalBackoffLimitNanos;

    public ReentrantGetAndSetLockWithBackoff() {
        this(100, 1_000_000, TimeUnit.MICROSECONDS);
    }

    ReentrantGetAndSetLockWithBackoff(long initiaBackoffLimit, long finalBackoffLimit, TimeUnit timeUnit) {
        checkArgument(initiaBackoffLimit < finalBackoffLimit);

        this.initialBackoffLimitNanos = timeUnit.toNanos(initiaBackoffLimit);
        this.finalBackoffLimitNanos = timeUnit.toNanos(finalBackoffLimit);
    }

    @Override
    public void lock() {
        long limitNanos = initialBackoffLimitNanos;
        while (true) {
            var currentOwner = owner.getOpaque();
            if (currentOwner == Thread.currentThread()) {
                entries++;
                break;
            } else if (currentOwner == null) {
                if (owner.weakCompareAndSetAcquire(null, Thread.currentThread())) {
                    entries++;
                    break;
                } else {
                    sleepNanos(ThreadLocalRandom.current().nextLong(limitNanos));
                    limitNanos = Math.min(finalBackoffLimitNanos, 2 * limitNanos);
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public void unlock() {
        if (Thread.currentThread() != owner.getOpaque()) {
            throw new IllegalMonitorStateException("Attempted to unlock without holding the lock");
        }

        if (--entries == 0) {
            owner.setRelease(null);
        }
    }

    @Override
    public boolean isReentrant() {
        return true;
    }

    private void sleepNanos(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }
}
