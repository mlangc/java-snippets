package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * This benchmark demonstrates how casting overhead can impact loops iterating over lists.
 *
 * <p>
 * All benchmark methods are prefixed with `groupX`. Always look at results from one group at a time.
 * Comparing results from different groups doesn't make sense if you are interested in the overhead
 * incurred by implicit casts. Here is a brief summary of the different groups:
 *
 *     <ul>
 *         <li>group0 compares reading a reference to reading and an implicit cast</li>
 *         <li>group1 compares copying a reference to copying and an implicit cast</li>
 *         <li>group2 compares calling hashCode to calling hashCode and an implicit cast</li>
 *         <li>group3 does the same as group2, but iterates over <code>Object[]</code> and <code>Integer[]</code></li>
 *     </ul>
 * </p>
 */
@Fork(1)
//@Fork(value = 1, jvmArgsPrepend = "-XX:CompileCommand=print,at.mlangc.benchmarks.ListCastingOverheadBenchmark::group3*")
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class ListCastingOverheadBenchmark {
    @Param("100000")
    private int length;

    private ArrayList<Integer> integerArrayList;
    private ArrayList<Object> objectArrayList;
    private ArraySink arraySink;

    private Object[] objectArray;
    private Integer[] integerArray;

    @Setup
    public void setup() {
        integerArrayList = new ArrayList<>(length);
        objectArrayList = new ArrayList<>(length);
        arraySink = new ArraySink(length);
        objectArray = new Object[length];
        integerArray = new Integer[length];

        var rng = new Random(123);
        for (int i = 0; i < length; i++) {
            integerArrayList.add(rng.nextInt());
            objectArrayList.add(integerArrayList.getLast());
            objectArray[i] = integerArrayList.getLast();
            integerArray[i] = integerArrayList.getLast();
        }
    }

    @Benchmark
    public void group0ConsumeObjectArrayList(Blackhole bh) {
        for (Object o : objectArrayList) {
            bh.consume(o);
        }
    }

    @Benchmark
    public void group0ConsumeIntegerArrayList(Blackhole bh) {
        for (Integer i : integerArrayList) {
            bh.consume(i);
        }
    }


    @Benchmark
    public ArraySink group1CopyReferencesIntegerArrayList() {
        arraySink.reset();
        for (Integer i : integerArrayList) {
            arraySink.consumeInteger(i);
        }

        return arraySink;
    }

    @Benchmark
    public ArraySink group1CopyReferencesObjectArrayList() {
        arraySink.reset();
        for (Object o : objectArrayList) {
            arraySink.consumeObject(o);
        }

        return arraySink;
    }

    @Benchmark
    public void group2ConsumeIntegerArrayListHashCodes(Blackhole bh) {
        for (Integer i : integerArrayList) {
            bh.consume(i.hashCode());
        }
    }

    @Benchmark
    public void group2ConsumeObjectArrayListHashCodes(Blackhole bh) {
        for (Object o : objectArrayList) {
            bh.consume(o.hashCode());
        }
    }

    @Benchmark
    public void group3ConsumeIntegerArrayHashCodes(Blackhole bh) {
        for (Integer i : integerArray) {
            bh.consume(i.hashCode());
        }
    }

    @Benchmark
    public void group3ConsumeObjectArrayHashCodes(Blackhole bh) {
        for (Object o : objectArray) {
            bh.consume(o.hashCode());
        }
    }

    /**
     * A custom sink, that stores objects in an internal array.
     *
     * @implNote objects are written in reverse order to prevent smart compilers from figuring out that they could
     * use an equivalent of {@link System#arraycopy(Object, int, Object, int, int)} under certain conditions.
     */
    public static final class ArraySink {
        final Object[] values;
        int pos;

        ArraySink(int size) {
            this.values = new Object[size];
            this.pos = values.length - 1;
        }

        void consumeObject(Object o) {
            values[pos--] = o;
        }

        void consumeInteger(Integer i) {
            values[pos--] = i;
        }

        void reset() {
            pos = values.length - 1;
        }
    }
}
