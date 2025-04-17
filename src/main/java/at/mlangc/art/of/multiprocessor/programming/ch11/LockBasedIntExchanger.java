package at.mlangc.art.of.multiprocessor.programming.ch11;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedIntExchanger implements IntExchanger {
    private final Lock lock = new ReentrantLock(true);
    private final Condition ready = lock.newCondition();

    private Integer storedValue;
    private Thread owner1;
    private Thread owner2;

    @Override
    public Response exchange(Integer x, long timeout, TimeUnit unit) {
        var remainingNanos = unit.toNanos(timeout);

        while (true) {
            lock.lock();
            try {
                if (owner1 == null) {
                    owner1 = Thread.currentThread();
                    storedValue = x;
                    ready.signal();
                } else if (owner2 == null && owner1 != Thread.currentThread()) {
                        owner2 = Thread.currentThread();
                        var ret = new Exchanged(storedValue);
                        storedValue = x;
                        ready.signalAll();
                        return ret;
                    } else if (owner2 != null && Thread.currentThread() == owner1) {
                        var ret = new Exchanged(storedValue);
                        owner1 = null;
                        owner2 = null;
                        storedValue = null;
                        ready.signal();
                        return ret;
                } else {
                    if (remainingNanos > 0) {
                        remainingNanos = awaitReadyNanos(remainingNanos);
                    } else if (owner2 == null) {
                        owner1 = null;
                        storedValue = null;
                        ready.signal();
                        return Response.TIMED_OUT;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private long awaitReadyNanos(long nanos) {
        try {
            return ready.awaitNanos(nanos);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }
}
