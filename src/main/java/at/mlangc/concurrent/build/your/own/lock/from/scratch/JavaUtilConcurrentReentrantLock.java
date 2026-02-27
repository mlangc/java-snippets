package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JavaUtilConcurrentReentrantLock implements SimpleLock {
    private static final Lock lock = new ReentrantLock();

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
