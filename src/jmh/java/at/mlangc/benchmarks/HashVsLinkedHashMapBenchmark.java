package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Fork(value = 1)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class HashVsLinkedHashMapBenchmark {

    public enum Impl {
        HASH, LINKED;

        <K, V> Map<K, V> newMap(int numMappings, float loadFactor) {
            return switch (this) {
                case HASH -> new HashMap<>(calculateMapCapacity(numMappings, loadFactor), loadFactor);
                case LINKED -> new LinkedHashMap<>(calculateMapCapacity(numMappings, loadFactor), loadFactor);
            };
        }

        <K, V> Map<K, V> newMap() {
            return switch (this) {
                case HASH -> new HashMap<>();
                case LINKED -> new LinkedHashMap<>();
            };
        }

        static int calculateMapCapacity(int numMappings, float loadFactor) {
            return (int) Math.ceil(numMappings / (double) loadFactor);
        }
    }

    @State(Scope.Thread)
    public static class CopyToArrayState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"0.5", "0.75"})
        float loadFactor;

        Map<Integer, Integer> map;

        Object[] array;

        @Setup
        public void setup() {
            map = impl.newMap(size, loadFactor);
            array = new Object[size];

            var rng = new SplittableRandom(313);
            while (map.size() < size)
                map.put(rng.nextInt(), rng.nextInt());
        }
    }

    @Benchmark
    public Object[] copyToArray(CopyToArrayState state) {
        return state.map.entrySet().toArray(state.array);
    }

    @State(Scope.Thread)
    public static class GetAnyEntryState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"0.5", "0.75"})
        float loadFactor;

        SplittableRandom rng;

        Map<?, ?>[] maps;

        @Setup
        public void setup() {
            rng = new SplittableRandom(313);

            maps = IntStream.range(0, 128)
                    .mapToObj(_ -> {
                        var map = impl.newMap(size, loadFactor);
                        while (map.size() < size) {
                            map.put(rng.nextInt(), rng.nextInt());
                        }
                        return map;
                    }).toArray(Map<?, ?>[]::new);
        }
    }

    @Benchmark
    public Map.Entry<?, ?> getAnyEntry(GetAnyEntryState state) {
        var map = state.maps[state.rng.nextInt(state.maps.length)];
        if (map instanceof LinkedHashMap<?,?> linkedHashMap) {
            return linkedHashMap.firstEntry();
        } else {
            return map.entrySet().iterator().next();
        }
    }

    @Benchmark
    public Map.Entry<?, ?> getAnyEntryUsingIteratorNext(GetAnyEntryState state) {
        var map = state.maps[state.rng.nextInt(state.maps.length)];
        return map.entrySet().iterator().next();
    }

    @State(Scope.Thread)
    public static class PutBenchmarkState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"0.5", "0.75"})
        float loadFactor;

        @Param("1.5")
        float keySpread;

        int numKeys;

        SplittableRandom rng;

        @Setup
        public void setup() {
            rng = new SplittableRandom(313);

            if (keySpread < 1.01) {
                throw new IllegalArgumentException();
            }

            numKeys = Math.toIntExact(Math.round(Math.ceil(size * keySpread)));
        }
    }

    @Benchmark
    public Map<Integer, Boolean> put(PutBenchmarkState state) {
        var map = state.impl.<Integer, Boolean>newMap(state.size, state.loadFactor);
        do {
            map.put(state.rng.nextInt(state.numKeys), Boolean.TRUE);
        } while (map.size() < state.size);

        return map;
    }

    @State(Scope.Thread)
    public static class GetBenchmarkState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"0.5", "0.75"})
        float loadFactor;

        @Param("1.5")
        float keySpread;

        int numKeys;

        SplittableRandom rng;

        Map<Integer, Integer> map;

        @Setup
        public void setup() {
            rng = new SplittableRandom(313);

            if (keySpread < 1.01) {
                throw new IllegalArgumentException();
            }

            numKeys = Math.toIntExact(Math.round(Math.ceil(size * keySpread)));

            map = impl.newMap(size, loadFactor);
            do {
                map.put(rng.nextInt(numKeys), rng.nextInt());
            } while (map.size() < size);
        }
    }

    @Benchmark
    public Integer get(GetBenchmarkState state) {
        return state.map.get(state.rng.nextInt(state.numKeys));
    }

    @State(Scope.Thread)
    public static class MergeState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"1.05", "1.5", "5"})
        float keySpread;

        int numKeys;

        @Setup
        public void setup() {
            if (keySpread < 1.01) {
                throw new IllegalArgumentException();
            }

            numKeys = Math.toIntExact(Math.round(Math.ceil(size * keySpread)));
        }
    }

    @Benchmark
    public Map<Integer, Integer> merge(MergeState state) {
        var rng = new SplittableRandom(313);
        var map = state.impl.<Integer, Integer>newMap();

        do {
            map.merge(rng.nextInt(state.numKeys), rng.nextInt(-1, 2), (a, b) -> {
                var c = a + b;
                return c == 0 ? null : c;
            });
        } while (map.size() < state.size);

        return map;
    }

    @State(Scope.Thread)
    public static class AccumulateAndCopyToArrayState {
        @Param
        Impl impl;

        @Param({"5", "50000"})
        int size;

        @Param({"1.05", "1.5", "5"})
        float keySpread;

        int numKeys;

        Map<Integer, Long> map;

        Object[] array;

        @Setup
        public void setup() {
            if (keySpread < 1.01) {
                throw new IllegalArgumentException();
            }

            numKeys = Math.toIntExact(Math.round(Math.ceil(size * keySpread)));
            array = new Object[size];
        }
    }

    @Benchmark
    public Object[] accumulateAndCopyToArray(AccumulateAndCopyToArrayState state) {
        var rng = new SplittableRandom(313);
        var map = state.impl.<Integer, Long>newMap();

        do {
            map.merge(rng.nextInt(state.numKeys), 1L, Long::sum);
        } while (map.size() < state.size);

        return map.entrySet().toArray(state.array);
    }
}
