package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CopilotCastingBenchmarkV2 {

    private List<Object> objectList;
    private List<String> stringList;
    private int index;

    @Setup(Level.Iteration)
    public void setup() {
        stringList = new ArrayList<>();
        objectList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String value = "value" + i;
            stringList.add(value);
            objectList.add(value); // stored as Object
        }
        index = 0;
    }

    @Benchmark
    public void castFromObject(Blackhole bh) {
        Object obj = objectList.get(index++ % objectList.size());
        bh.consume((String) obj); // cast always succeeds
    }

    @Benchmark
    public void noCastFromString(Blackhole bh) {
        String str = stringList.get(index++ % stringList.size());
        bh.consume(str); // no cast
    }
}