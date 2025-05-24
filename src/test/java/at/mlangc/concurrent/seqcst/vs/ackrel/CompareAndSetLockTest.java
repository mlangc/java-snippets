package at.mlangc.concurrent.seqcst.vs.ackrel;

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