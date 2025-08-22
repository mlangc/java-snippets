package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CopilotCastingBenchmarkV1 {

    private List<Object> objectList;
    private List<String> stringList;

    @Setup
    public void setup() {
        stringList = new ArrayList<>();
        objectList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String value = "value" + i;
            stringList.add(value);
            objectList.add(value); // same values, but stored as Object
        }
    }

    @Benchmark
    public String castFromObject() {
        String result = null;
        for (Object obj : objectList) {
            result = (String) obj; // cast always succeeds
        }
        return result;
    }

    @Benchmark
    public String noCastFromString() {
        String result = null;
        for (String str : stringList) {
            result = str; // no cast needed
        }
        return result;
    }
}
