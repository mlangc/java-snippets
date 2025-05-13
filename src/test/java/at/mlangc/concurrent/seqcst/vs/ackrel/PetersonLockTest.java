package at.mlangc.concurrent.seqcst.vs.ackrel;

class PetersonLockTest extends IndexedLockTest {
    @Override
    IndexedLock newLock() {
        return new PetersonLock();
    }
}