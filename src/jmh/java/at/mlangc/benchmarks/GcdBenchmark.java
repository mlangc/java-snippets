package at.mlangc.benchmarks;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static at.mlangc.benchmarks.GcdImpls.euclidIterative;
import static at.mlangc.benchmarks.GcdImpls.euclidRecursive;


@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class GcdBenchmark {
    private final Random rng = new Random(42);
    private final long[] longs = rng.longs(1000, Long.MIN_VALUE + 1, Long.MAX_VALUE).toArray();

    @Benchmark
    public void recursiveGcd(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(euclidRecursive(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void iterativeGcd(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(euclidIterative(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void commonsGcd(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(ArithmeticUtils.gcd(longs[i], longs[i + 1]));
        }
    }
}
