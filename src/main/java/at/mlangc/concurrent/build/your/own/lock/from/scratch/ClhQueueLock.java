package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ClhQueueLock implements SimpleLock {
    private final ThreadLocal<AtomicBoolean> myPred = new ThreadLocal<>();
    private final ThreadLocal<AtomicBoolean> myNode = ThreadLocal.withInitial(() -> new AtomicBoolean(true));
    private final AtomicReference<AtomicBoolean> endOfQueue = new AtomicReference<>(new AtomicBoolean());

    @Override
    public void lock() {
        var me = myNode.get();
        var pred = endOfQueue.getAndSet(me);
        myPred.set(pred);

        while (pred.getAcquire()) {
            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        var pred = myPred.get();
        pred.setPlain(true);

        myNode.get().setRelease(false);
        myNode.set(pred);
    }
}
