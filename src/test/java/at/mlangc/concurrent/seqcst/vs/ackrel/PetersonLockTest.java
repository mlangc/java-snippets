package at.mlangc.concurrent.seqcst.vs.ackrel;

class PetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new PetersonLock(MemoryOrdering.VOLATILE);
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