package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicLong;

public class CompareAndSetLock implements SimpleLock {
    private final AtomicLong owner = new AtomicLong(-1);

    @Override
    public void lock() {
        while (!owner.compareAndSet(-1, Thread.currentThread().threadId())) {
            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        owner.set(-1);
    }
}
