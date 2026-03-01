package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class McsLockWithInterestingError implements SimpleLock {
    private static class Node {
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicReference<Node> behind = new AtomicReference<>();
    }

    private final ThreadLocal<Node> perThreadNode = ThreadLocal.withInitial(Node::new);
    private final AtomicReference<Node> tail = new AtomicReference<>();

    @Override
    public void lock() {
        var myNode = perThreadNode.get();
        myNode.locked.setPlain(false);

        var waitingBeforeMe = tail.getAndSet(myNode);
        if (waitingBeforeMe == null) {
            return;
        }

        waitingBeforeMe.behind.setRelease(myNode);

        while (!myNode.locked.getAcquire()) {
            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        var myNode = perThreadNode.get();

        if (tail.compareAndSet(myNode, null)) {
            return;
        }

        while (true) {
            var waitingAfterMe = myNode.behind.getAcquire();

            if (waitingAfterMe == null) {
                Thread.onSpinWait();
            } else {
                waitingAfterMe.locked.setRelease(true);
                break;
            }
        }
    }
}
