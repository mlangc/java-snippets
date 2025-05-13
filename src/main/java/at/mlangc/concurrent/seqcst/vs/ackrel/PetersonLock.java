package at.mlangc.concurrent.seqcst.vs.ackrel;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

class PetersonLock extends IndexedLock {
    private final MemoryOrdering memoryOrdering;
    private final AtomicInteger victim = new AtomicInteger(-1);
    private final AtomicIntegerArray locked = new AtomicIntegerArray(2);

    PetersonLock() {
        this(MemoryOrdering.SEQ_CST);
    }

    PetersonLock(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
    }

    public void lock() {
        var idx = ThreadIndex.current();

        setLocked(idx, true);
        setVictim(idx);

        while (getLocked(1 - idx) && getVictim() == idx) {
            Thread.onSpinWait();
        }
    }

    public void unlock() {
        int idx = ThreadIndex.current();
        Preconditions.checkState(getLocked(idx));
        setLocked(idx, false);
    }

    @Override
    int threadLimit() {
        return 2;
    }

    private boolean getLocked(int i) {
        return memoryOrdering.read(locked, i) == 1;
    }

    private void setLocked(int index, boolean value) {
        memoryOrdering.write(locked, index, value ? 1 : 0);
    }

    private int getVictim() {
        return memoryOrdering.read(victim);
    }

    private void setVictim(int idx) {
        memoryOrdering.write(victim, idx);
    }
}
