package at.mlangc.art.of.multiprocessor.programming.ch9;

import java.util.concurrent.atomic.AtomicMarkableReference;

class ConcurrentSortedLinkedInts implements SortedLinkedInts {
    private static class Node {
        final int x;
        final AtomicMarkableReference<Node> next;

        Node(int x) {
            this(x, null);
        }

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicMarkableReference<>(next, false);
        }
    }

    private final Node head = new Node(0);

    @Override
    public boolean add(int x) {
        var markHolder = new boolean[1];

        var last = head;
        var current = head;

        while (true) {
            current = skipAndRemoveTillUnmarked(last, markHolder);
            if (current == null || current.x > x) {
                var newNode = new Node(x, current);
                if (last.next.weakCompareAndSet(current, newNode, false, false)) {
                    return true;
                } else {
                    continue;
                }
            } else if (current.x == x) {
                return false;
            }

            last = current;
        }
    }

    private Node skipAndRemoveTillUnmarked(Node current, boolean[] markHolder) {
        var current0 = current;
        Node current1 = null;
        var skipped = false;

        while (true) {
            current = current.next.get(markHolder);
            if (current1 == null) {
                current1 = current;
            }

            if (markHolder[0]) {
                skipped = true;
            } else {
                if (skipped && !current0.next.weakCompareAndSet(current1, current, true, false)) {
                    current = current0;
                    skipped = false;
                } else {
                    return current;
                }
            }
        }
    }

    @Override
    public boolean remove(int x) {
        var markHolder = new boolean[1];

        var last = head;
        var current = head;
        while (true) {
            current = skipAndRemoveTillUnmarked(last, markHolder);

            if (current == null || current.x > x) {
                return false;
            } else if (current.x == x) {
                if (last.next.weakCompareAndSet(current, current, false, true)) {
                    return true;
                } else {
                    continue;
                }
            }

            last = current;
        }
    }

    @Override
    public boolean contains(int x) {
        var markHolder = new boolean[1];
        var current = head;

        while ((current = current.next.get(markHolder)) != null) {
            if (current.x > x) {
                return false;
            }

            if (current.x == x) {
                return !markHolder[0];
            }
        }

        return false;
    }
}
