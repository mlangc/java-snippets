package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class ModExpBenchmark {
    @Param(value = {"15", "30"})
    private long power;

    private long mod;

    private long mask;

    @Param(value = "1000")
    private int calls;

    private long[] as;
    private long[] bs;

    private final ModExpImpls impls = new ModExpImpls();

    @Setup
    public void setup() {
        mod = 1L << power;
        mask = mod - 1;

        var rng = new Random(42);
        as = rng.longs(calls, 1, mod).toArray();
        bs = rng.longs(calls, 0, Long.MAX_VALUE).toArray();
    }

    @Benchmark
    public void modExpClassic(Blackhole blackhole) {
        for (int i = 0; i < as.length; i++) {
            blackhole.consume(impls.modPowClassic(as[i], bs[i], mod));
        }
    }

    @Benchmark
    public void maskExpClassic(Blackhole blackhole) {
        for (int i = 0; i < as.length; i++) {
            blackhole.consume(impls.maskPowClassic(as[i], bs[i], mask));
        }
    }

    @Benchmark
    public void modExpBranchLess(Blackhole blackhole) {
        for (int i = 0; i < as.length; i++) {
            blackhole.consume(impls.modPowBranchLess(as[i], bs[i], mod));
        }
    }

    @Benchmark
    public void maskExpBranchLess(Blackhole blackhole) {
        for (int i = 0; i < as.length; i++) {
            blackhole.consume(impls.maskPowBranchLess(as[i], bs[i], mask));
        }
    }
}
