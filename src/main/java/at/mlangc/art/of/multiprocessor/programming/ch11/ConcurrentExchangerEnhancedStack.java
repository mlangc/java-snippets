package at.mlangc.art.of.multiprocessor.programming.ch11;

import at.mlangc.art.of.multiprocessor.programming.ch11.IntExchanger.Exchanged;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentExchangerEnhancedStack implements IntStack {
    private static final long TIMEOUT_NANOS = TimeUnit.MICROSECONDS.toNanos(1);

    private static class Node {
        final int value;
        Node next;

        private Node(int value) {
            this.value = value;
        }
    }

    private final AtomicReference<Node> head = new AtomicReference<>();

    private final IntExchanger exchanger;

    public ConcurrentExchangerEnhancedStack(IntExchanger exchanger) {
        this.exchanger = exchanger;
    }

    @Override
    public void push(int x) {
        var newHead = new Node(x);
        while (true) {
            var oldHead = head.getAcquire();
            newHead.next = oldHead;

            if (head.weakCompareAndSetRelease(oldHead, newHead)) {
                return;
            } else if (exchanger.exchange(x, TIMEOUT_NANOS, TimeUnit.NANOSECONDS) instanceof Exchanged(Integer exchanged)) {
                if (exchanged == null) {
                    return;
                }
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public int pop() {
        while (true) {
            var oldHead = head.getAcquire();

            if (oldHead == null) {
                throw new NoSuchElementException();
            }

            if (head.weakCompareAndSetRelease(oldHead, oldHead.next)) {
                return oldHead.value;
            } else if (exchanger.exchange(null, TIMEOUT_NANOS, TimeUnit.NANOSECONDS) instanceof Exchanged(Integer exchanged)) {
                if (exchanged != null) {
                    return exchanged;
                }
            }

            Thread.onSpinWait();
        }
    }
}
