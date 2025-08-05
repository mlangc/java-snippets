package at.mlangc.concurrent.ack.rel;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.atomic.AtomicInteger;

@Description("Demonstrates the limitations of acquire-release mode")
@JCStressTest
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "both actors observe 0")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE)
@State
public class ReleaseAcquireRaceDemoSimplified {
    final AtomicInteger x = new AtomicInteger();
    final AtomicInteger y = new AtomicInteger();

    @Actor
    public void actor1(II_Result r) {
        x.setRelease(1);
        r.r1 = y.getAcquire();
    }

    @Actor
    public void actor2(II_Result r) {
        y.setRelease(1);
        r.r2 = x.getAcquire();
    }
}
