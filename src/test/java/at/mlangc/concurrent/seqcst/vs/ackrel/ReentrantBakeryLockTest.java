package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;

class ReentrantBakeryLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new ReentrantBakeryLock(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                MemoryOrdering.VOLATILE);
    }
}