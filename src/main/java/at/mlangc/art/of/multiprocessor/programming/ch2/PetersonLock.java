package at.mlangc.art.of.multiprocessor.programming.ch2;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

class PetersonLock {
    private enum MemoryOrdering {
        SEQ_CST, ACQ_REL;
    }

    private static final MemoryOrdering MEMORY_ORDERING = MemoryOrdering.SEQ_CST;

    private final AtomicInteger victim = new AtomicInteger(-1);
    private final AtomicIntegerArray locked = new AtomicIntegerArray(2);

    void lock() {
        var id = threadId();

        setLocked(id, true);
        setVictim(id);

        while (getLocked(1 - id) && getVictim() == id) {
            Thread.onSpinWait();
        }
    }

    void unlock() {
        int id = threadId();
        Preconditions.checkState(getLocked(id));
        setLocked(id, false);
    }

    private static int threadId() {
        int id = Integer.parseInt(Thread.currentThread().getName().split(":")[1]);
        Preconditions.checkState(0 <= id && id < 2);
        return id;
    }

    private boolean getLocked(int i) {
        return (MEMORY_ORDERING == MemoryOrdering.SEQ_CST ? locked.get(i) : locked.getAcquire(i)) == 1;
    }

    private void setLocked(int index, boolean value) {
        if (MEMORY_ORDERING == MemoryOrdering.SEQ_CST) {
            locked.set(index, value ? 1 : 0);
        } else {
            locked.setRelease(index, value ? 1 : 0);

        }
    }

    private int getVictim() {
        return MEMORY_ORDERING == MemoryOrdering.SEQ_CST ? victim.get() : victim.getAcquire();
    }

    private void setVictim(int id) {
        if (MEMORY_ORDERING == MemoryOrdering.SEQ_CST) {
            victim.set(id);
        } else {
            victim.setRelease(id);
        }
    }
}
