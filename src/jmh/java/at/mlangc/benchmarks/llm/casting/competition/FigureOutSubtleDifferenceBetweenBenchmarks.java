package at.mlangc.benchmarks.llm.casting.competition;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @implNote a very subtle difference in the setup method of these benchmarks lead to measurable
 * differences in performance. It seems to make a difference, if objects are exclusively owned
 * or shared by lists/arrays we iterate over.
 */
@BenchmarkMode(Mode.Throughput)
@Fork(1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
public class FigureOutSubtleDifferenceBetweenBenchmarks {
    private static final int SIZE = 10000;
    private static final int SEED = 1234;

    @State(Scope.Benchmark)
    public static class GeminiState {
        private Object[] objectsArray;
        private ArrayList<Integer> arrayList;

        @Setup
        public void setup() {
            Random rng = new Random(SEED);
            objectsArray = new Object[SIZE];
            arrayList = new ArrayList<>(SIZE);

            for (int i = 0; i < SIZE; i++) {
                arrayList.add(rng.nextInt());
                objectsArray[i] = arrayList.getLast();
            }
        }
    }

    @State(Scope.Benchmark)
    public static class DeepseekState {
        private Object[] objectsArray;
        private ArrayList<Integer> arrayList;

        @Setup
        public void setup() {
            Random rng = new Random(SEED);
            objectsArray = new Object[SIZE];
            arrayList = new ArrayList<>(SIZE);
            for (int i = 0; i < SIZE; i++) {
                var v = rng.nextInt();
                arrayList.add(v);
                objectsArray[i] = v;
            }
        }
    }

    @Benchmark
    public int sumArrayGemini(GeminiState state) {
        int sum = 0;
        for (Object o : state.objectsArray) {
            sum += (Integer) o;
        }
        return sum;
    }

    @Benchmark
    public int sumListGemini(GeminiState state) {
        int sum = 0;
        for (Integer i : state.arrayList) {
            sum += i;
        }
        return sum;
    }

    @Benchmark
    public int sumArrayDeepseek(DeepseekState state) {
        int sum = 0;
        for (Object o : state.objectsArray) {
            sum += (Integer) o;
        }
        return sum;
    }

    @Benchmark
    public int sumListDeepseek(DeepseekState state) {
        int sum = 0;
        for (Integer i : state.arrayList) {
            sum += i;
        }
        return sum;
    }
}
