package at.mlangc.concurrent.seqcst.vs.ackrel;

class GetAndSetLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new GetAndSetLock(MemoryOrdering.PLAIN);
    }
}