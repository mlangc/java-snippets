package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsAppend = {"-XX:CompileCommand=print,at.mlangc.benchmarks.*::iterate", "-XX:CompileCommand=dontinline,at.mlangc.benchmarks.ArrayListIterationBenchmark::addNotInlined", "-XX:+PrintFlagsFinal"})
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class ArrayListIterationBenchmark {
    @Param({"1000"})
    private int size;

    @Param({"COLON"})
    private IterationStyle style;

    @Param({"false"})
    private boolean addInlined;

    private ArrayList<Integer> list;
    private Integer[] integerArray;
    private Object[] objectArray;
    private int[] intArray;

    public enum IterationStyle {
        INT_ARRAY {
            @Override
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = 0;
                for (int elem : benchmark.intArray) {
                    sum = benchmark.add(sum, elem);
                }

                return sum;
            }
        },
        INTEGER_ARRAY {
            @Override
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = 0;
                for (Integer elem : benchmark.integerArray) {
                    sum = benchmark.add(sum, elem);
                }

                return sum;
            }
        },
        OBJECT_ARRAY {
            @Override
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = 0;
                for (Object elem : benchmark.objectArray) {
                    sum = benchmark.add(sum, (Integer) elem);
                }

                return sum;
            }
        },
        COLON {
            @Override
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = 0;
                for (Integer elem : benchmark.list) {
                    sum = benchmark.add(sum, elem);
                }

                return sum;
            }
        },
        FOR_EACH {
            @Override
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = new Object() {
                    int x;
                };

                benchmark.list.forEach(elem -> sum.x = benchmark.add(sum.x, elem));
                return sum.x;
            }
        },
        INDEXED {
            int iterate(ArrayListIterationBenchmark benchmark) {
                var sum = 0;
                for (int i = 0; i < benchmark.list.size(); i++) {
                    sum = benchmark.add(sum, benchmark.list.get(i));
                }
                return sum;
            }
        };

        abstract int iterate(ArrayListIterationBenchmark benchmark);
    }

    @Setup
    public void setup() {
        list = new ArrayList<>(size);
        integerArray = new Integer[size];
        objectArray = new Object[size];
        intArray = new int[size];

        for (int i = 0; i < size; i++) {
            list.add(i);
            integerArray[i] = i;
            objectArray[i] = i;
            intArray[i] = i;
        }
    }

    @Benchmark
    public int run() {
        return style.iterate(this);
    }

    private int add(int a, int b) {
        if (addInlined) {
            return a + b;
        } else {
            return addNotInlined(a, b);
        }
    }

    /**
     * @apiNote only not inlined if the appropriate -XX:CompileCommand is used!
     */
    private static int addNotInlined(int a, int b) {
        return a + b;
    }
}
