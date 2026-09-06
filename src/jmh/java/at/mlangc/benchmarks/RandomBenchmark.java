package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class RandomBenchmark {
    final Random random = new Random(0);
    final SplittableRandom splittableRandom = new SplittableRandom(0);

    @Benchmark
    public int randomInt() {
        return random.nextInt();
    }

    @Benchmark
    public int splittableRandomInt() {
        return splittableRandom.nextInt();
    }

    @Benchmark
    public boolean randomBoolean() {
        return random.nextBoolean();
    }

    @Benchmark
    public boolean splittableRandomBoolean() {
        return splittableRandom.nextBoolean();
    }
}
