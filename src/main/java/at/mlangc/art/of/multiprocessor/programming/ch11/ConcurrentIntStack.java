package at.mlangc.art.of.multiprocessor.programming.ch11;

import at.mlangc.art.of.multiprocessor.programming.ch7.ExponentialBackoff;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentIntStack implements IntStack {
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(1);
    private static final Duration BACKOFF_CAP = Duration.ofMillis(500);

    private static class Node {
        final int x;
        Node next;

        Node(int x) {
            this.x = x;
        }
    }

    private final AtomicReference<Node> head = new AtomicReference<>();

    @Override
    public void push(int x) {
        var newHead = new Node(x);
        ExponentialBackoff backoff = null;

        while (true) {
            var currentHead = head.get();
            newHead.next = currentHead;

            if (head.weakCompareAndSetVolatile(currentHead, newHead)) {
                return;
            }

            backoff = getOrCreateBackoff(backoff);
            backoff.backoff();
        }
    }

    @Override
    public int pop() {
        ExponentialBackoff backoff = null;
        while (true) {
            var oldHead = head.get();
            if (oldHead == null) {
                throw new NoSuchElementException();
            }

            var newHead = oldHead.next;
            if (head.weakCompareAndSetVolatile(oldHead, newHead)) {
                return oldHead.x;
            }

            backoff = getOrCreateBackoff(backoff);
            backoff.backoff();
        }
    }

    private static ExponentialBackoff getOrCreateBackoff(ExponentialBackoff backoff) {
        if (backoff == null) {
            backoff = new ExponentialBackoff(INITIAL_BACKOFF, BACKOFF_CAP);
        }

        return backoff;
    }
}
