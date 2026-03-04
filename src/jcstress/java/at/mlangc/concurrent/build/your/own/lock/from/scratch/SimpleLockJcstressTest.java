package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@State
@Outcome(id = "5", expect = Expect.ACCEPTABLE)
@Outcome(id = "", expect = Expect.FORBIDDEN)
public class SimpleLockJcstressTest {
    final SimpleLock lock = new BrokenNoopLock();

    int x;

    @Actor
    public void actor1() {
        lock.runWithLock(() -> x++);
    }

    @Actor
    public void actor2() {
        if (!lock.isReentrant()) {
            lock.runWithLock(() -> x++);
        } else {
            lock.runWithLock(() -> lock.runWithLock(() -> x++));
        }
    }

    @Actor
    public void actor3() {
        if (!lock.isReentrant()) {
            lock.runWithLock(() -> x++);
        } else {
            lock.runWithLock(() -> lock.runWithLock(() -> lock.runWithLock(() -> x++)));
        }
    }

    @Actor
    public void actor4() {
        if (!lock.isReentrant()) {
            lock.runWithLock(() -> x++);
        } else {
            lock.runWithLock(() -> lock.runWithLock(() -> lock.runWithLock(() -> lock.runWithLock(() -> x++))));
        }
    }

    @Actor
    public void actor5() {
        try {
            lock.lock();
            x++;
            lock.unlock();
            if (lock.hasCheckedUnlock()) {
                lock.unlock();
                throw new AssertionError("Should never reach this line");
            }
        } catch (IllegalMonitorStateException e) {
            if (!lock.hasCheckedUnlock()) {
                throw e;
            }
        }
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
