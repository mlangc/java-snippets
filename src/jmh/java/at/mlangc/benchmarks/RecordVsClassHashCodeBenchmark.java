package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class RecordVsClassHashCodeBenchmark {
    record TwoObjectsRecord(Object o1, Object o2) { }
    record IntegerStringRecord(Integer i, String s) { }

    @SuppressWarnings("ClassCanBeRecord")
    static class TwoObjectClass {
        final Object o1;
        final Object o2;

        TwoObjectClass(Object o1, Object o2) {
            this.o1 = o1;
            this.o2 = o2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            TwoObjectClass that = (TwoObjectClass) o;
            return Objects.equals(o1, that.o1) && Objects.equals(o2, that.o2);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(o1);
            result = 31 * result + Objects.hashCode(o2);
            return result;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    static class IntegerStringClass {
        final Integer i;
        final String s;

        IntegerStringClass(Integer i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            IntegerStringClass that = (IntegerStringClass) o;
            return Objects.equals(i, that.i) && Objects.equals(s, that.s);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(i);
            result = 31 * result + Objects.hashCode(s);
            return result;
        }
    }

    TwoObjectsRecord twoObjectsRecord = new TwoObjectsRecord(1, "a");
    TwoObjectClass twoObjectsClass = new TwoObjectClass(1, "a");
    IntegerStringRecord integerStringRecord = new IntegerStringRecord(1, "a");
    IntegerStringClass integerStringClass = new IntegerStringClass(1, "a");

    @Benchmark
    public int hashTwoObjectsRecord() {
        return twoObjectsRecord.hashCode();
    }

    @Benchmark
    public int hashTwoObjectsClass() {
        return twoObjectsClass.hashCode();
    }

    @Benchmark
    public int hashIntegerStringRecord() {
        return integerStringRecord.hashCode();
    }

    @Benchmark
    public int hashIntegerStringClass() {
        return integerStringClass.hashCode();
    }
}
