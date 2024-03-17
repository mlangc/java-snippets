package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class PrimitivesCmpBenchmark {
    @State(Scope.Benchmark)
    public static class StateTemplate {
        final Random rng = new Random(42);

        @Param("500")
        int numPairs;

        @Param("0.5")
        double chanceEqual;
    }

    @State(Scope.Benchmark)
    public static class IntsToCompareToEachOther extends StateTemplate {
        int[] values;

        @Setup
        public void setup() {
            values = new int[2 * numPairs];
            for (int i = 0; i < numPairs; i++) {
                values[2 * i] = rng.nextInt();

                if (rng.nextDouble() < chanceEqual) {
                    values[2 * i + 1] = values[2 * i];
                } else {
                    do {
                        values[2 * i + 1] = rng.nextInt();
                    } while (values[2 * i] == values[2 * i + 1]);
                }
            }
        }
    }

    @State(Scope.Benchmark)
    public static class LongsToCompareToEachOther extends StateTemplate {
        long[] values;

        @Setup
        public void setup() {
            values = new long[2 * numPairs];
            for (int i = 0; i < numPairs; i++) {
                values[2 * i] = rng.nextLong();

                if (rng.nextDouble() < chanceEqual) {
                    values[2 * i + 1] = values[2 * i];
                } else {
                    do {
                        values[2 * i + 1] = rng.nextLong();
                    } while (values[2 * i] == values[2 * i + 1]);
                }
            }
        }
    }

    @State(Scope.Benchmark)
    public static class IntsToCompareAgainstZero extends StateTemplate {
        int[] values;

        @Setup
        public void setup() {
            values = new int[2 * numPairs];

            for (int i = 0; i < numPairs; i++) {
                if (rng.nextDouble() >= chanceEqual) {
                    do {
                        values[2 * i] = rng.nextInt();
                    } while (values[2 * i] == 0);
                }
            }
        }
    }

    @State(Scope.Benchmark)
    public static class LongsToCompareAgainstZero extends StateTemplate {
        long[] values;

        @Setup
        public void setup() {
            values = new long[2 * numPairs];

            for (int i = 0; i < numPairs; i++) {
                if (rng.nextDouble() >= chanceEqual) {
                    do {
                        values[2 * i] = rng.nextLong();
                    } while (values[2 * i] == 0);
                }
            }
        }
    }

    @State(Scope.Benchmark)
    public static class LongsToCompareAgainstNonZero extends StateTemplate {
        long[] values;

        @Param("313")
        long value;

        @Setup
        public void setup() {
            values = new long[2 * numPairs];

            for (int i = 0; i < numPairs; i++) {
                if (rng.nextDouble() >= chanceEqual) {
                    do {
                        values[2 * i] = rng.nextLong();
                    } while (values[2 * i] == 42);
                } else {
                    values[2 * i] = 42;
                }
            }
        }
    }

    @Benchmark
    @SuppressWarnings("DuplicatedCode")
    public int compareArbitraryInts(IntsToCompareToEachOther ints) {
        var res = 0;
        var values = ints.values;

        for (int i = 0; i < values.length; i += 2) {
            res += values[i] == values[i + 1] ? 1 : 0;
        }

        assertThat((double) res).isBetween(0.5 * ints.chanceEqual * ints.numPairs, 1.5 * ints.chanceEqual * ints.numPairs);
        return res;
    }

    @Benchmark
    @SuppressWarnings("DuplicatedCode")
    public int compareArbitraryLongs(LongsToCompareToEachOther longs) {
        var res = 0;
        var values = longs.values;

        for (int i = 0; i < values.length; i += 2) {
            res += values[i] == values[i + 1] ? 1 : 0;
        }

        assertThat((double) res).isBetween(0.5 * longs.chanceEqual * longs.numPairs, 1.5 * longs.chanceEqual * longs.numPairs);
        return res;
    }

    @Benchmark
    @SuppressWarnings("DuplicatedCode")
    public int compareIntsAgainstZero(IntsToCompareAgainstZero ints) {
        var res = 0;
        var values = ints.values;

        for (int i = 0; i < values.length; i += 2) {
            res += values[i] == 0 ? 1 : 0;
        }

        var expectedMin = 0.5 * ints.chanceEqual * ints.numPairs;
        var expectedMax = 1.5 * ints.chanceEqual * ints.numPairs;

        if (res < expectedMin || res > expectedMax) {
            throw new AssertionError(STR."expectedMin=\{expectedMin}, expectedMax=\{expectedMax}, res=\{res}");
        }

        return res;
    }

    @Benchmark
    @SuppressWarnings("DuplicatedCode")
    public int compareLongsAgainstZero(LongsToCompareAgainstZero longs) {
        var res = 0;
        var values = longs.values;

        for (int i = 0; i < values.length; i += 2) {
            res += values[i] == 0 ? 1 : 0;
        }

        var expectedMin = 0.5 * longs.chanceEqual * longs.numPairs;
        var expectedMax = 1.5 * longs.chanceEqual * longs.numPairs;

        if (res < expectedMin || res > expectedMax) {
            throw new AssertionError(STR."expectedMin=\{expectedMin}, expectedMax=\{expectedMax}, res=\{res}");
        }

        return res;
    }

    @Benchmark
    @SuppressWarnings("DuplicatedCode")
    public int compareLongsAgainst42(LongsToCompareAgainstNonZero longs) {
        var res = 0;
        var values = longs.values;

        for (int i = 0; i < values.length; i += 2) {
            res += values[i] == 42 ? 1 : 0;
        }

        assertThat((double) res)
                .isBetween(0.5 * longs.chanceEqual * longs.numPairs, 1.5 * longs.chanceEqual * longs.numPairs);
        return res;
    }
}
