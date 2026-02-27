package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicReference;

public class ReentrantGetAndSetLock implements SimpleLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private int entries;

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
}
