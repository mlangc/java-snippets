package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class TweakableBakeryLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new TweakableBakeryLock(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1), MemoryOrdering.ACQUIRE_RELEASE, true);
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