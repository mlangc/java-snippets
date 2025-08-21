package at.mlangc.vmlens;

import at.mlangc.concurrent.seqcst.vs.ackrel.MemoryOrdering;
import at.mlangc.concurrent.seqcst.vs.ackrel.PetersonLock;
import at.mlangc.concurrent.seqcst.vs.ackrel.ThreadIndex;
import com.vmlens.api.AllInterleavings;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

public class PetersonLockVmLensTest {
    @Test
    void shouldNotDetectProblemWithWorkingLock() throws InterruptedException {
        try (var allInterleavings = new AllInterleavings("petersonOk")) {
            var lock = new PetersonLock(MemoryOrdering.VOLATILE);
            var x = new MutableInt();

            while (allInterleavings.hasNext()) {
                Thread first = new Thread(ThreadIndex.toName(0)) {
                    @Override
                    public void run() {
                        lock.lock();
                        x.increment();
                        lock.unlock();
                    }
                };

                Thread second = new Thread(ThreadIndex.toName(1)) {
                    @Override
                    public void run() {
                        lock.lock();
                        x.increment();
                        lock.unlock();
                    }
                };

                first.start();
                second.start();
                second.join();
                first.join();
            }
        }
    }

    @Test
    void shouldDetectProblemWithBrokenLock() throws InterruptedException {
        try (var allInterleavings = new AllInterleavings("petersonNok")) {
            var lock = new PetersonLock(MemoryOrdering.ACQUIRE_RELEASE);
            var x = new MutableInt();

            while (allInterleavings.hasNext()) {
                Thread first = new Thread(ThreadIndex.toName(0)) {
                    @Override
                    public void run() {
                        lock.lock();
                        x.increment();
                        lock.unlock();
                    }
                };

                Thread second = new Thread(ThreadIndex.toName(1)) {
                    @Override
                    public void run() {
                        lock.lock();
                        x.increment();
                        lock.unlock();
                    }
                };

                first.start();
                second.start();
                second.join();
                first.join();
            }
        }
    }

}
