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
        while (true) {
            var window = findAndRemoveMarked(x, markHolder);
            if (window.current != null && window.current.x == x) {
                return false;
            }

            var newNode = new Node(x, window.current);
            if (window.last.next.compareAndSet(window.current, newNode, false, false)) {
                return true;
            }
        }
    }

    private record Window(Node last, Node current) { }

    private Window findAndRemoveMarked(int x, boolean[] markHolder) {
        Node last;
        Node current;

        outer: while (true) {
            last = head;
            current = last.next.getReference();

            while (current != null) {
                var next = current.next.get(markHolder);
                if (!markHolder[0]) {
                    if (current.x >= x) {
                        break;
                    }

                    last = current;
                    current = next;
                } else {
                    if (last.next.compareAndSet(current, next, false, false)) {
                        current = next;
                    } else {
                        continue outer;
                    }
                }
            }

            break;
        }

        return new Window(last, current);
    }

    @Override
    public boolean remove(int x) {
        var markHolder = new boolean[1];

        while (true) {
            var window = findAndRemoveMarked(x, markHolder);
            if (window.current == null || window.current.x != x) {
                return false;
            }

            var next = window.current.next.getReference();
            if (window.current.next.compareAndSet(next, next, false, true)) {
                window.last.next.compareAndSet(window.current, next, false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        var markHolder = new boolean[1];
        var current = head.next.get(markHolder);

        while (true) {
            if (current == null || current.x > x) {
                return false;
            }

            var next = current.next.get(markHolder);
            if (current.x == x) {
                return !markHolder[0];
            }

            current = next;
        }
    }
}
