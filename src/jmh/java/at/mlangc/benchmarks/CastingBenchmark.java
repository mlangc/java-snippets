package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class CastingBenchmark {
    private int[] ints;
    private Integer[] integers;
    private Object[] objects;
    private List<Integer> integerList;
    private int i;
    private int sum;

    @Param({"10000"})
    private int length;

    @Param({"100"})
    private int bound;

    @Setup
    public void setup() {
        var rng = new Random(123);

        ints = new int[length];
        integers = new Integer[length];
        objects = new Object[length];
        integerList = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            ints[i] = rng.nextInt(bound);
            integers[i] = ints[i];
            objects[i] = integers[i];
            integerList.add(integers[i]);
        }

    }

    @Benchmark
    public int sumInts() {
        sum += ints[i++];
        if (i == length) i = 0;
        return sum;
    }

    @Benchmark
    public int sumIntegers() {
        sum += integers[i++];
        if (i == length) i = 0;
        return sum;
    }

    @Benchmark
    public int sumObjects() {
        sum += (Integer) objects[i++];
        if (i == length) i = 0;
        return sum;
    }

    @Benchmark
    public int sumIntegerList() {
        sum += integerList.get(i);
        if (i == length) i = 0;
        return sum;
    }
}
