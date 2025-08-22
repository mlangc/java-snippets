package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class CompareAndSetLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new CompareAndSetLock(MemoryOrdering.ACQUIRE_RELEASE);
    }

    @Override
    boolean hasCheckedUnlock() {
        return false;
    }
}