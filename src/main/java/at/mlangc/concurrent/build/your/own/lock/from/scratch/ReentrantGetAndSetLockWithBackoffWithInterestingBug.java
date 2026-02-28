package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static com.google.common.base.Preconditions.checkArgument;

public class ReentrantGetAndSetLockWithBackoffWithInterestingBug implements SimpleLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private int entries;

    private long maxBackoffNanos;
    private final long initialMaxBackoffNanos;
    private final long finalMaxBackoffNanos;

    public ReentrantGetAndSetLockWithBackoffWithInterestingBug() {
        this(100, 1_000_000, TimeUnit.MICROSECONDS);
    }

    ReentrantGetAndSetLockWithBackoffWithInterestingBug(long initialMaxBackoff, long finalMaxBackoff, TimeUnit timeUnit) {
        checkArgument(initialMaxBackoff < finalMaxBackoff);

        this.initialMaxBackoffNanos = timeUnit.toNanos(initialMaxBackoff);
        this.finalMaxBackoffNanos = timeUnit.toNanos(finalMaxBackoff);
    }

    @Override
    public void lock() {
        while (true) {
            var currentOwner = owner.getOpaque();
            if (currentOwner == Thread.currentThread()) {
                entries++;
                break;
            } else if (currentOwner == null && owner.weakCompareAndSetAcquire(null, Thread.currentThread())) {
                entries++;
                break;
            } else {
                if (currentOwner != null) {
                    if (maxBackoffNanos == 0) {
                        maxBackoffNanos = initialMaxBackoffNanos;
                    } else {
                        maxBackoffNanos = Math.min(finalMaxBackoffNanos, 2 * maxBackoffNanos);
                    }

                    LockSupport.parkNanos(ThreadLocalRandom.current().nextLong(maxBackoffNanos));
                }

                Thread.onSpinWait();
            }
        }

        maxBackoffNanos = 0;
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
}
