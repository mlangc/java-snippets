package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class McsLock implements SimpleLock {
    private static final int SPINS_BEFORE_PARK = 25_000;

    private static class Node {
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicReference<Node> behind = new AtomicReference<>();
        final Thread owner = Thread.currentThread();
        long entries;
    }

    private final ThreadLocal<Node> perThreadNode = ThreadLocal.withInitial(Node::new);
    private final AtomicReference<Node> tail = new AtomicReference<>();

    @Override
    public void lock() {
        var myNode = perThreadNode.get();

        if (!myNode.locked.getPlain()) {
            var waitingBeforeMe = tail.getAndSet(myNode);
            if (waitingBeforeMe != null) {
                waitingBeforeMe.behind.setRelease(myNode);

                var spins = 0;
                while (!myNode.locked.getAcquire()) {
                    if (++spins < SPINS_BEFORE_PARK) {
                        Thread.onSpinWait();
                    } else {
                        spins = 0;
                        LockSupport.park(this);
                    }
                }
            }
        }

        myNode.locked.setPlain(true);
        myNode.entries++;
    }

    @Override
    public void unlock() {
        var myNode = perThreadNode.get();
        if (!myNode.locked.getPlain()) {
            throw new IllegalMonitorStateException("Not locked by unlocking thread");
        }

        if (--myNode.entries != 0) {
            return;
        }

        if (myNode.behind.getPlain() == null) {
            if (tail.compareAndSet(myNode, null)) {
                myNode.locked.setPlain(false);
                return;
            }
        }

        while (true) {
            var waitingForMe = myNode.behind.getAcquire();

            if (waitingForMe == null) {
                Thread.onSpinWait();
            } else {
                myNode.behind.setPlain(null);
                myNode.locked.setPlain(false);
                waitingForMe.locked.setRelease(true);
                LockSupport.unpark(waitingForMe.owner);
                break;
            }
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
