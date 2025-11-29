package at.mlangc.concurrent.util.map;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@JCStressTest
@Description("Maps under stress")
@Outcome(id = "1, 1, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 1, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 0, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 1, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "1, 0, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 1, 0", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 0, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "0, 0, 0", expect = Expect.ACCEPTABLE)
@State
public class MapPutRemoveTest {
    static final int BOUND = 32;

    final Map<Integer, Integer> map = new HashMap<>();

    @Actor
    public void actor1(III_Result r) {
        for (int i = 0; i < BOUND; i++) {
            var k = ThreadLocalRandom.current().nextInt(BOUND);
            try {
                var v = map.putIfAbsent(k, k);
                r.r1 = Math.max(r.r1, (v == null || v.equals(k)) ? 0 : 2);
            } catch (Exception _) {
                r.r1 = Math.max(r.r1, 1);
            }
        }
    }

    @Actor
    public void actor2(III_Result r) {
        for (int i = 0; i < BOUND; i++) {
            var k = ThreadLocalRandom.current().nextInt(BOUND);

            try {
                var v = map.remove(k);
                r.r2 = Math.max(r.r2, (v == null || v.equals(k)) ? 0 : 2);
            } catch (Exception _) {
                r.r2 = Math.max(r.r2, 1);
            }
        }
    }

    @Actor
    public void actor3(III_Result r) {
        for (int i = 0; i < BOUND; i++) {
            var k = ThreadLocalRandom.current().nextInt(BOUND);

            try {
                var v = map.computeIfAbsent(k, Function.identity());
                r.r3 = Math.max(r.r3, v.equals(k) ? 0 : 2);
            } catch (Exception _) {
                r.r3 = Math.max(r.r3, 1);
            }
        }
    }

    @Actor
    public void actor4() {
        for (int i = 0; i < BOUND; i++) {
            try {
                if (ThreadLocalRandom.current().nextInt(BOUND) == 0) {
                    map.clear();
                } else {
                    Thread.onSpinWait();
                }
            } catch (Exception _) {

            }
        }
    }
}
