package at.mlangc.concurrent.seqcst.vs.ackrel;

class BakeryLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new BakeryLock(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), MemoryOrdering.SEQ_CST);
    }
}