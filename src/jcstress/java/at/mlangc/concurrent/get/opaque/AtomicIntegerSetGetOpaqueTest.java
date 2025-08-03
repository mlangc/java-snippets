package at.mlangc.concurrent.get.opaque;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import java.util.concurrent.atomic.AtomicInteger;

@JCStressTest
@Description("Demonstrates the behaviour of opaque mode")
@Outcome(id = "0, 0, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 0, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 0, 2", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 1, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 1, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 1, 2", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 0, 0", expect = Expect.FORBIDDEN, desc = "b=1, then b=0")
@Outcome(id = "1, 0, 1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "b=1, then a=0")
@Outcome(id = "1, 0, 2", expect = Expect.ACCEPTABLE_INTERESTING, desc = "b=1, then a=0")
@Outcome(id = "1, 1, 0", expect = Expect.FORBIDDEN, desc = "b=1, then b=0")
@Outcome(id = "1, 1, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 1, 2", expect = Expect.ACCEPTABLE)
@Outcome(id = "2, 0, 0", expect = Expect.FORBIDDEN, desc = "b=2, then b=0")
@Outcome(id = "2, 0, 1", expect = Expect.FORBIDDEN, desc = "b=2, then b=1")
@Outcome(id = "2, 0, 2", expect = Expect.ACCEPTABLE_INTERESTING, desc = "b=2, then a=0")
@Outcome(id = "2, 1, 0", expect = Expect.FORBIDDEN, desc = "b=2, then b=0")
@Outcome(id = "2, 1, 1", expect = Expect.FORBIDDEN, desc = "b=2, then b=1")
@Outcome(id = "2, 1, 2", expect = Expect.ACCEPTABLE)
@State
public class AtomicIntegerSetGetOpaqueTest {
    final AtomicInteger a = new AtomicInteger();
    final AtomicInteger b = new AtomicInteger();

    @Actor
    public void actor1() {
        a.setOpaque(1);
        b.setOpaque(1);
        b.setOpaque(2);
    }

    @Actor
    public void actor2(III_Result r) {
        r.r1 = b.getOpaque();
        r.r2 = a.getOpaque();
        r.r3 = b.getOpaque();
    }
}
