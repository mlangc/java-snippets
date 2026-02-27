package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicLong;

public class GetAndSetLock implements SimpleLock {
    private final AtomicLong owner = new AtomicLong(-1);

    @Override
    public void lock() {
        var threadId = Thread.currentThread().threadId();

        while (true) {
            var currentOwner = owner.getOpaque();
            if (currentOwner == threadId) {
                break;
            } else if (currentOwner == -1 && owner.weakCompareAndSetAcquire(-1, threadId)) {
                break;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public void unlock() {
        owner.setRelease(-1);
    }
}
