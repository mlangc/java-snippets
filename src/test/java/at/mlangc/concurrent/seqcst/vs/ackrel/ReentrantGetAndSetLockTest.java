package at.mlangc.concurrent.seqcst.vs.ackrel;

class ReentrantGetAndSetLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new ReentrantGetAndSetLock();
    }
}