package at.mlangc.concurrent.seqcst.vs.ackrel;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicIntegerArray;

class BakeryLock extends IndexedLock {
    private final MemoryOrdering memoryOrdering;
    private final AtomicIntegerArray flags;
    private final AtomicIntegerArray labels;

    BakeryLock(int maxThreads) {
        this(maxThreads, MemoryOrdering.SEQ_CST);
    }

    BakeryLock(int maxThreads, MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
        this.flags = new AtomicIntegerArray(maxThreads);
        this.labels = new AtomicIntegerArray(maxThreads);
    }

    @Override
    int threadLimit() {
        return flags.length();
    }

    @Override
    public void lock() {
        var idx = ThreadIndex.current();

        if (memoryOrdering.get(flags, idx) == 1) {
            return;
        }

        memoryOrdering.set(flags, idx, 1);
        var label = 0;
        for (int i = 0; i < labels.length(); i++) {
            label = Math.max(label, memoryOrdering.get(labels, i));
        }
        label++;

        memoryOrdering.set(labels, idx, label);

        while (true) {
            boolean locked = true;
            for (int i = 0; i < labels.length(); i++) {
                if (i != idx && memoryOrdering.get(flags, i) == 1) {
                    var otherLabel = memoryOrdering.get(labels, i);
                    if (otherLabel < label || otherLabel == label && i < idx) {
                        locked = false;
                        break;
                    }
                }
            }

            if (locked) {
                break;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public void unlock() {
        var idx = ThreadIndex.current();
        Preconditions.checkArgument(memoryOrdering.get(flags, idx) == 1);
        memoryOrdering.set(flags, idx, 0);
    }

    record LabelWithIndex(int label, int index) {

    }

    private LabelWithIndex findMinFlaggedLabel() {
        var label = Integer.MAX_VALUE;
        var index = -1;
        for (int i = 0; i < flags.length(); i++) {
            if (memoryOrdering.get(flags, i) == 1) {
                var label0 = memoryOrdering.get(labels, i);
                if (label > label0 ) {
                    label = label0;
                    index = i;
                }
            }
        }

        return index < 0 ? new LabelWithIndex(-1, -1) : new LabelWithIndex(label, index);
    }
}
