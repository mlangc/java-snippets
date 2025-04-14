package at.mlangc.art.of.multiprocessor.programming.ch10;

import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedUnboundedIntQueue implements IntQueue {
    private final Lock enqLock = new ReentrantLock();
    private final Lock deqLock = new ReentrantLock();

    private static class Node {
        final int x;
        volatile Node next;

        Node(int x) {
            this.x = x;
        }
    }

    private Node head = new Node(0);
    private Node tail = head;

    @Override
    public void enq(int x) {
        var node = new Node(x);
        enqLock.lock();
        try {
            tail.next = node;
            tail = node;
        } finally {
            enqLock.unlock();
        }
    }

    @Override
    public int deq() {
        deqLock.lock();
        try {
            if (head.next == null) {
                throw new NoSuchElementException();
            } else {
                head = head.next;
                return head.x;
            }
        } finally {
            deqLock.unlock();
        }
    }
}
