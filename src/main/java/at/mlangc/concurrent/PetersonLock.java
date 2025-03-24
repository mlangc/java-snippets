package at.mlangc.concurrent;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicIntegerArray;

class PetersonLock {
    private volatile long victim = -1;
    private final AtomicIntegerArray locked = new AtomicIntegerArray(2);

    void lock() {
        var id = threadId();

        locked.set(id, 1);
        victim = id;

        while (locked.get(1 - id) == 1 && victim == id) {
            Thread.onSpinWait();
        }
    }

    void unlock() {
        int id = threadId();
        Preconditions.checkState(locked.get(id) == 1);
        locked.set(id, 0);
    }

    private static int threadId() {
        int id = Integer.parseInt(Thread.currentThread().getName().split(":")[1]);
        Preconditions.checkState(0 <= id && id < 2);
        return id;
    }
}
