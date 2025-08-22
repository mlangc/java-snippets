package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class BakeryLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new BakeryLock(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                MemoryOrdering.VOLATILE);
    }

    @Override
    boolean isReentrant() {
        return false;
    }

    @Override
    boolean hasCheckedUnlock() {
        return false;
    }
}