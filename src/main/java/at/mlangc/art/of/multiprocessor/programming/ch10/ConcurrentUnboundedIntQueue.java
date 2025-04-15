package at.mlangc.art.of.multiprocessor.programming.ch10;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentUnboundedIntQueue implements IntQueue {
    private static class Node {
        final int x;
        final AtomicReference<Node> next = new AtomicReference<>();

        Node(int x) {
            this.x = x;
        }
    }

    private final AtomicReference<Node> head = new AtomicReference<>(new Node(-1));
    private final AtomicReference<Node> tail = new AtomicReference<>(head.getPlain());

    @Override
    public void enq(int x) {
        var node = new Node(x);

        while (true) {
            var currentTail = tail.get();
            var previousNext = currentTail.next.compareAndExchange(null, node);

            if (previousNext != null) {
                tail.compareAndSet(currentTail, previousNext);
            } else {
                tail.compareAndSet(currentTail, node);
                break;
            }
        }
    }

    @Override
    public int deq() {
        while (true) {
            var currentHead = head.get();
            var currentNext = currentHead.next.get();

            if (currentNext == null) {
                throw new NoSuchElementException();
            }

            if (head.compareAndSet(currentHead, currentNext)) {
                return currentNext.x;
            }
        }
    }
}
