package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class McsLock implements SimpleLock {
    private static class Node {
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicReference<Node> behind = new AtomicReference<>();
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

                while (!myNode.locked.getAcquire()) {
                    Thread.onSpinWait();
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
            var waitingAfterMe = myNode.behind.getAcquire();

            if (waitingAfterMe == null) {
                Thread.onSpinWait();
            } else {
                myNode.behind.setPlain(null);
                myNode.locked.setPlain(false);
                waitingAfterMe.locked.setRelease(true);
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
