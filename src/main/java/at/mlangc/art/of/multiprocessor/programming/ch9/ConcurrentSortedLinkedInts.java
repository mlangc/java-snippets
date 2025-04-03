package at.mlangc.art.of.multiprocessor.programming.ch9;

class ConcurrentSortedLinkedInts implements SortedLinkedInts {
    private static class Node {
        int x;
        Node next;
    }

    private final Node head = new Node();

    @Override
    public boolean add(int x) {
        var node = find(x);

        if (node != null && node.x == x) {
            return false;
        }

        if (node == null) {
            node = head;
        }

        var next = new Node();
        next.x = x;
        next.next = node.next;
        node.next = next;
        return true;
    }

    @Override
    public boolean remove(int x) {
        var node = head.next;
        var prev = head;

        while (node != null) {
            if (node.x == x) {
                prev.next = node.next;
                return true;
            } else if (node.x > x) {
                return false;
            }

            prev = node;
            node = node.next;
        }

        return false;
    }

    @Override
    public boolean contains(int x) {
        var node = find(x);
        return node != null && node.x == x;
    }

    private Node find(int x) {
        var next = head.next;
        Node prev = null;

        while (next != null) {
            if (next.x == x) {
                return next;
            } else if (next.x > x) {
                return prev;
            } else {
                prev = next;
                next = next.next;
            }
        }

        return prev;
    }
}
