package at.mlangc.concurrent.seqcst.vs.ackrel;

class ReentrantPetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new ReentrantPetersonLock(MemoryOrdering.VOLATILE);
    }
}