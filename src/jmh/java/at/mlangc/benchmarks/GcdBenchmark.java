package at.mlangc.benchmarks;

import com.google.common.math.LongMath;
import org.apache.commons.numbers.core.ArithmeticUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class GcdBenchmark {
    private final Random rng = new Random(42);
    private final long[] longs = rng.longs(2000, Long.MIN_VALUE + 1, Long.MAX_VALUE).map(Math::abs).toArray();

    @Benchmark
    public void bigInteger(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(
                    BigInteger.valueOf(longs[i]).gcd(BigInteger.valueOf(longs[i + 1])).longValue());
        }
    }

    @Benchmark
    public void euclidRecursive(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.euclidRecursive(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void euclidIterative(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.euclidIterative(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinApache(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(ArithmeticUtils.gcd(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinGuava(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(LongMath.gcd(longs[i], longs[i + 1]));
        }
    }
    @Benchmark
    public void steinPositive1(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdSteinPositive1(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinPositive2(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdSteinPositive2(longs[i], longs[i + 1]));
        }
    }
    @Benchmark
    public void steinPositive3(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdSteinPositive3(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinPositive4(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdSteinPositive4(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinPositive5(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdSteinPositive5(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void stein(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdStein(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinApacheTweaked(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdApacheTweaked(longs[i], longs[i + 1]));
        }
    }

    @Benchmark
    public void steinGuavaTweaked(Blackhole blackhole) {
        for (int i = 0; i < longs.length; i += 2) {
            blackhole.consume(GcdImpls.gcdGuavaTweaked(longs[i], longs[i + 1]));
        }
    }
}
