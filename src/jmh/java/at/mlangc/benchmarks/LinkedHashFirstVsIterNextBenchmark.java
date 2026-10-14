package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LinkedHashFirstVsIterNextBenchmark {
    @Param({"10", "100"})
    int size;

    LinkedHashMap<Integer, Boolean> linkedHashMap;

    @Setup
    public void setup() {
        var rng = new Random(42);

        linkedHashMap = new LinkedHashMap<>();

        do {
            Integer key = rng.nextInt();
            linkedHashMap.putFirst(key, Boolean.TRUE);
        } while (linkedHashMap.size() < size);
    }

    @Benchmark
    public Object firstEntry() {
        return linkedHashMap.firstEntry();
    }

    @Benchmark
    public Object entryIteratorNext() {
        return linkedHashMap.entrySet().iterator().next();
    }
}
