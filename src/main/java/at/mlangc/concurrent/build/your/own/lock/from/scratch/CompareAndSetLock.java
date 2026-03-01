package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicReference;

public class CompareAndSetLock implements SimpleLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private long entries;

    @Override
    public void lock() {
        if (owner.getPlain() != Thread.currentThread()) {
            while (!owner.weakCompareAndSetAcquire(null, Thread.currentThread())) {
                Thread.onSpinWait();
            }
        }

        entries++;
    }

    @Override
    public void unlock() {
        if (owner.get() != Thread.currentThread()) {
            throw new IllegalMonitorStateException("Not locked");
        }

        if (--entries == 0) {
            owner.setRelease(null);
        }
    }

    @Override
    public boolean hasCheckedUnlock() {
        return true;
    }

    @Override
    public boolean isReentrant() {
        return true;
    }
}
