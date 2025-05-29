package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class TweakablePetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new TweakablePetersonLock(MemoryOrdering.ACQUIRE_RELEASE, true);
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