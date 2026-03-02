package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class JavaUtilConcurrentReentrantLock implements SimpleLock {
    private final Lock lock;

    JavaUtilConcurrentReentrantLock() {
        this(false);
    }

    JavaUtilConcurrentReentrantLock(boolean fair) {
        this.lock = new ReentrantLock(fair);
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public boolean isReentrant() {
        return true;
    }
}
