package at.mlangc.concurrent.build.your.own.lock.from.scratch;

interface SimpleLock {
    void lock();
    void unlock();

    default boolean isReentrant() {
        return false;
    }

    default boolean hasCheckedUnlock() {
        return false;
    }

    default void runWithLock(Runnable op) {
        lock();

        try {
            op.run();
        } finally {
            unlock();
        }
    }
}
