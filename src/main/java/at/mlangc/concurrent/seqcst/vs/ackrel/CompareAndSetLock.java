package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.atomic.AtomicInteger;

public class CompareAndSetLock extends IndexedLock {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger locked = new AtomicInteger(-1);

    public CompareAndSetLock(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    @Override
    public void lock() {
        var myIdx = ThreadIndex.current();

        while (true) {
            var oIdx = memoryOrdering.get(locked);

            if (oIdx == myIdx) {
                break;
            } else if (memoryOrdering.get(locked) == -1) {
                if (memoryOrdering.weakCompareAndSet(locked, -1, myIdx)) {
                    break;
                }
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        memoryOrdering.set(locked, -1);
    }

    @Override
    int threadLimit() {
        return Integer.MAX_VALUE;
    }
}
