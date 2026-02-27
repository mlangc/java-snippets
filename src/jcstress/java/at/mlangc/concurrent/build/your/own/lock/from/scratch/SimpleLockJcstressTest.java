package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@State
@Outcome(id = "4", expect = Expect.ACCEPTABLE)
@Outcome(id = "", expect = Expect.FORBIDDEN)
public class SimpleLockJcstressTest {
    final SimpleLock lock = new CompareAndSetLock();

    int x;

    @Actor
    public void actor1() {
        lock.runWithLock(() -> x++);
    }

    @Actor
    public void actor2() {
        lock.runWithLock(() -> x++);
    }

    @Actor
    public void actor3() {
        lock.runWithLock(() -> x++);
    }

    @Actor
    public void actor4() {
        lock.runWithLock(() -> x++);
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
