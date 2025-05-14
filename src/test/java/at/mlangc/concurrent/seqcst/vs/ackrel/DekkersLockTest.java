package at.mlangc.concurrent.seqcst.vs.ackrel;

class DekkersLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new DekkersLock();
    }
}