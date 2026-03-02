package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

class ReentrantLikeQueueLock implements SimpleLock {
    private final ConcurrentLinkedQueue<Thread> waiters = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final boolean fair;

    ReentrantLikeQueueLock() {
        this(false);
    }

    ReentrantLikeQueueLock(boolean fair) {
        this.fair = fair;
    }

    @Override
    public void lock() {
        if ((!fair || waiters.isEmpty()) && owner.weakCompareAndSetVolatile(null, Thread.currentThread())) {
            return;
        }

        waiters.add(Thread.currentThread());
        while (waiters.peek() != Thread.currentThread() || !owner.compareAndSet(null, Thread.currentThread())) {
            LockSupport.park(this);
        }

        waiters.remove();
    }

    @Override
    public void unlock() {
        owner.set(null);

         var next = waiters.peek();
         if (next != null) {
             LockSupport.unpark(next);
         }
    }
}
