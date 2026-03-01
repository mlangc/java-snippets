package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicReference;

public class FancyClhQueueLock implements SimpleLock {
    private static class Node {
        Node(boolean locked) {
            this.owner = Thread.currentThread();
            this.locked = locked;
        }

        final Thread owner;
        volatile boolean locked;
        volatile Node next;
    }

    private final AtomicReference<Node> tail = new AtomicReference<>(new Node(false));

    @Override
    public void lock() {
        var myNode = new Node(true);
        var next = tail.getAndSet(myNode);
        myNode.next = next;

        while (next.locked) {
            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        Node prev = null;
        var node = tail.get();

        while (true) {
            if (node.owner == Thread.currentThread()) {
                if (prev != null) {
                    prev.next = null;
                }

                node.locked = false;
                break;
            } else {
                prev = node;
                while ((node = prev.next) == null) {
                    Thread.onSpinWait();
                }
            }
        }
    }

}
