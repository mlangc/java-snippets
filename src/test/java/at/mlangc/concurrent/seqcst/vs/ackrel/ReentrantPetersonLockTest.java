package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class ReentrantPetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new ReentrantPetersonLock(MemoryOrdering.VOLATILE);
    }
}