package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ClhQueueWithHashMapLock implements SimpleLock {
    private final Map<Thread, AtomicBoolean> nodes = new IdentityHashMap<>();
    private final AtomicReference<AtomicBoolean> endOfQueue = new AtomicReference<>(new AtomicBoolean());

    @Override
    public void lock() {
        var me = new AtomicBoolean(true);
        var end = endOfQueue.getAndSet(me);

        while (end.getAcquire()) {
            Thread.onSpinWait();
        }

        nodes.put(Thread.currentThread(), me);
    }

    @Override
    public void unlock() {
        nodes.remove(Thread.currentThread()).setRelease(false);
    }
}
