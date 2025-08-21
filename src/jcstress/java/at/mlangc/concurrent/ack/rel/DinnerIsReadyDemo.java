package at.mlangc.concurrent.ack.rel;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.atomic.AtomicBoolean;

@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING)
@Outcome(id = "42", expect = Expect.ACCEPTABLE_INTERESTING)
@Outcome(id = "43", expect = Expect.ACCEPTABLE)
@State
public class DinnerIsReadyDemo {
    final AtomicBoolean ready = new AtomicBoolean();
    int dinner;

    @Actor
    void actor1() {
        ready.set(true);
        dinner = 42;
    }

    @Actor
    void actor2(I_Result r) {
        while (!ready.get()) {
            Thread.onSpinWait();
        }

        dinner++;
        r.r1 = dinner;
    }
}
