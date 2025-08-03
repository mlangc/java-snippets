package at.mlangc.concurrent.get.opaque;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@JCStressTest
@Description("Shows why opaque mode must not used to publish objects with non final fields")
@Outcome(id = "2", expect = Expect.ACCEPTABLE)
@Outcome(id = "1", expect = Expect.ACCEPTABLE)
@Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "observed partially constructed object")
@State
public class AtomicReferenceSetGetOpaqueTest {
    static class Holder {
        int x = ThreadLocalRandom.current().nextInt(1, 3);
    }

    final AtomicReference<Holder> ref = new AtomicReference<>();

    @Actor
    public void actor1() {
        ref.setOpaque(new Holder());
    }

    @Actor
    public void actor2(I_Result r) {
        while (true) {
            var h = ref.getOpaque();
            if (h == null) {
                Thread.onSpinWait();
                continue;
            }

            r.r1 = h.x;
            return;
        }
    }
}
