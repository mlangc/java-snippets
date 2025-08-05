package at.mlangc.concurrent.ack.rel;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.util.concurrent.atomic.AtomicBoolean;

@Description("Demonstrates the limitations of acquire-release mode")
@JCStressTest
@Outcome(id = "false, false", expect = Expect.ACCEPTABLE)
@Outcome(id = "true, false", expect = Expect.ACCEPTABLE)
@Outcome(id = "false, true", expect = Expect.ACCEPTABLE)
@Outcome(id = "true, true", expect = Expect.ACCEPTABLE_INTERESTING, desc = "both actors win")
@State
public class ReleaseAcquireRaceDemo {
    final AtomicBoolean started1 = new AtomicBoolean();
    final AtomicBoolean started2 = new AtomicBoolean();

    @Actor
    void actor1(ZZ_Result r) {
        started1.setRelease(true);
        r.r1 = !started2.get();
    }

    @Actor
    void actor2(ZZ_Result r) {
        started2.setRelease(true);
        r.r2 = !started1.get();
    }
}
