package at.mlangc.benchmarks.llm.playground.copilot;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.Throughput) // Measures how many iterations per second
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1) // Keep it quick
@State(Scope.Thread)
public class CopilotBoxingOverheadBenchmark {

    @Param({"1000"}) // Number of elements to iterate over
    int size;

    private int[] primitiveArray;
    private List<Integer> boxedList;

    @Setup(Level.Iteration)
    public void setup() {
        primitiveArray = IntStream.range(0, size).toArray();
        boxedList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boxedList.add(i); // boxing happens here once
        }
    }

    @Benchmark
    public long iteratePrimitiveArray() {
        long sum = 0;
        for (int value : primitiveArray) {
            sum += value; // no boxing/unboxing
        }
        return sum;
    }

    @Benchmark
    public long iterateBoxedList() {
        long sum = 0;
        for (Integer value : boxedList) {
            sum += value; // unboxing happens here
        }
        return sum;
    }

    @Benchmark
    public long iterateBoxedListManualUnbox() {
        long sum = 0;
        for (int i = 0; i < boxedList.size(); i++) {
            sum += boxedList.get(i).intValue(); // explicit unboxing
        }
        return sum;
    }
}
