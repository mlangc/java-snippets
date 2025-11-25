package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class WhatIsOneMsBenchmark {
    @Benchmark
    public long randomNextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Benchmark
    public List<Object> newArrayList() {
        return new ArrayList<>();
    }
}
