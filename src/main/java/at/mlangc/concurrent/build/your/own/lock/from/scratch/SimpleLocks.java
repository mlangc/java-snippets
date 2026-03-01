package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.apache.commons.lang3.mutable.MutableInt;

class SimpleLocks {
    static SimpleLock makeReentrantAndChecked(SimpleLock lock) {
        if (lock.isReentrant() && lock.hasCheckedUnlock()) {
            return lock;
        }

        return new SimpleReentrantAndCheckedLock(lock);
    }

    private static class SimpleReentrantAndCheckedLock implements SimpleLock {
        private final SimpleLock lock;
        private final ThreadLocal<MutableInt> perThreadEntries = ThreadLocal.withInitial(MutableInt::new);

        public SimpleReentrantAndCheckedLock(SimpleLock lock) {
            this.lock = lock;
        }


        @Override
        public void lock() {
            var entries = perThreadEntries.get();
            if (entries.intValue() == 0) {
                lock.lock();
            }

            entries.increment();
        }

        @Override
        public void unlock() {
            var entries = perThreadEntries.get();

            if (entries.intValue() == 0) {
                throw new IllegalMonitorStateException("Attempt to unlock lock that isn't locked");
            }

            if (entries.decrementAndGet() == 0) {
                lock.unlock();
            }
        }

        @Override
        public boolean isReentrant() {
            return true;
        }

        @Override
        public boolean hasCheckedUnlock() {
            return true;
        }
    }

}
