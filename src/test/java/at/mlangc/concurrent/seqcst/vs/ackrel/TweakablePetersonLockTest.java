package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class TweakablePetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new TweakablePetersonLock(MemoryOrdering.VOLATILE, true);
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