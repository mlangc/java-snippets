package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, jvmArgsAppend = "-Dlog4j2.shutdownHookEnabled=true", jvm = "/Users/mlangc/.local/share/mise/installs/java/27.0.0/bin/java")
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class InDepthHashVsLinkedHashMapBenchmark {
    public enum MapImpl {
        HASH, LINKED, CONCURRENT;

        <K, V> Map<K, V> newInstance(float loadFactor, Map<K, V> entries) {
            Map<K, V> res = switch (this) {
                case HASH -> new HashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
                case LINKED -> new LinkedHashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
                case CONCURRENT -> new ConcurrentHashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
            };

            res.putAll(entries);
            return res;
        }

        <K, V> Map<K, V> newInstance(int initialCapacity, float loadFactor) {
            return switch (this) {
                case CONCURRENT -> new ConcurrentHashMap<>(initialCapacity, loadFactor);
                case HASH -> new HashMap<>(initialCapacity, loadFactor);
                case LINKED -> new LinkedHashMap<>(initialCapacity, loadFactor);
            };
        }

        <K> Set<K> newSetInstance(int initialCapacity, float loadFactor) {
            return switch (this) {
                case CONCURRENT -> Collections.newSetFromMap(new ConcurrentHashMap<>(initialCapacity, loadFactor));
                case HASH -> new HashSet<>(initialCapacity, loadFactor);
                case LINKED -> new LinkedHashSet<>(initialCapacity, loadFactor);
            };
        }

        <K, V> Map<K, V> newInstance() {
            return switch (this) {
                case CONCURRENT -> new ConcurrentHashMap<>();
                case HASH ->  new HashMap<>();
                case LINKED -> new LinkedHashMap<>();
            };
        }

        <K, V> Map<K, V> newInstance(int numMappings) {
            return newInstance(calculateMapCapacity(numMappings, 0.75f), 0.75f);
        }

        <K> Set<K> newSetInstance() {
            return switch (this) {
                case CONCURRENT -> ConcurrentHashMap.newKeySet();
                case HASH -> new HashSet<>();
                case LINKED -> new LinkedHashSet<>();
            };
        }

        <K> Set<K> newSetInstance(int numMappings) {
            return newSetInstance(calculateMapCapacity(numMappings, 0.75f), 0.75f);
        }

        static int calculateMapCapacity(int numMappings, float loadFactor) {
            return (int) Math.ceil(numMappings / (double) loadFactor);
        }
    }

    @State(Scope.Benchmark)
    public static class FillState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param({"12500", "25000", "50000"})
        int size;

        SplittableRandom rng;

        @Setup(Level.Iteration)
        public void setup() {
            rng = new SplittableRandom(313);
        }
    }

    @Benchmark
    public Map<Integer, Boolean> fill(FillState state) {
        var map = state.impl.<Integer, Boolean>newInstance();

        for (int i = 0; i < state.size; i++) {
            map.put(state.rng.nextInt(), Boolean.TRUE);
        }

        return map;
    }

    @State(Scope.Thread)
    public static class CopyEntriesState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param({"12500", "25000", "50000"})
        int size;

        Map<Integer, Boolean> map;
        Object[] entries;

        @Setup(Level.Trial)
        public void setup() {
            var rng = new SplittableRandom(313);
            map = impl.newInstance();

            while (map.size() < size) {
                map.put(rng.nextInt(), rng.nextBoolean());
            }

            entries = new Object[size];
        }
    }

    @Benchmark
    public Object[] copyEntries(CopyEntriesState state) {
        state.map.entrySet().toArray(state.entries);
        return state.entries;
    }

    @State(Scope.Thread)
    public static class CacheState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param("50000")
        int numQueries;

        @Param({"100", "1000", "10000" })
        int numKeys;

        Map<Integer, Integer> cache;

        List<Integer> evaluations;

        SplittableRandom rng;

        @Setup(Level.Iteration)
        public void setup() {
            cache = impl.newInstance(numKeys);
            evaluations = new ArrayList<>(numQueries);
            rng = new SplittableRandom(313);
        }
    }

    @Benchmark
    public List<Integer> cache(CacheState state) {
        state.cache.clear();
        state.evaluations.clear();

        for (int i = 0; i < state.numQueries; i++) {
            state.evaluations.add(state.cache.computeIfAbsent(state.rng.nextInt(state.numKeys), k -> k));
        }

        return state.evaluations;
    }

    @State(Scope.Thread)
    public static class AccumulateAndCopyState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param("50000")
        int numItems;

        @Param({"12500", "25000", "50000"})
        int numKeys;

        @Param({"true", "false"})
        boolean useDefaultCtor;

        SplittableRandom rng;

        List<Integer> list;

        Map<Integer, Integer> map;

        @Setup(Level.Iteration)
        public void setup() {
            rng = new SplittableRandom(42);
            list = new ArrayList<>(numKeys);

            if (!useDefaultCtor) {
                map = impl.newInstance(numKeys);
            }
        }
    }

    @Benchmark
    public List<Integer> accumulateAndCopyValues(AccumulateAndCopyState state) {
        state.list.clear();

        if (!state.useDefaultCtor) {
            state.map.clear();
        } else {
            state.map = state.impl.newInstance();
        }

        for (int i = 0; i < state.numItems; i++) {
            state.map.merge(state.rng.nextInt(state.numKeys), state.rng.nextInt(-1, 2), Integer::sum);
        }

        state.list.addAll(state.map.values());
        return state.list;
    }

    @State(Scope.Thread)
    public static class GetAnyEntryState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param({/*"12500", "25000",*/ "50000"})
        int size;

        @Param({/*"true",*/ "false"})
        boolean useDefaultCtor;

        Map<Integer, Boolean> map;

        @Setup
        public void setup() {
            if (useDefaultCtor) {
                map = impl.newInstance();
            } else {
                map = impl.newInstance(size);
            }

            var rng = new SplittableRandom(313);
            while (map.size() < size) {
                map.put(rng.nextInt(), Boolean.TRUE);
            }
        }
    }

    @Benchmark
    public static Object getAnyEntry(GetAnyEntryState state) {
        if (state.map instanceof LinkedHashMap<Integer, Boolean> linkedHashMap) {
            return linkedHashMap.firstEntry();
        } else {
            return state.map.entrySet().iterator().next();
        }
    }

    @State(Scope.Thread)
    public static class GetAnyKeyState {
        @Param({"HASH", "LINKED"})
        MapImpl impl;

        @Param({/*"12500", "25000",*/ "50000"})
        int size;

        @Param({/*"true",*/ "false"})
        boolean useDefaultCtor;

        Set<Integer> set;

        @Setup
        public void setup() {
            if (useDefaultCtor) {
                set = impl.newSetInstance();
            } else {
                set = impl.newSetInstance(size);
            }

            var rng = new SplittableRandom(313);
            while (set.size() < size) {
                set.add(rng.nextInt());
            }
        }
    }

    @Benchmark
    public static Object getAnyKey(GetAnyKeyState state) {
        if (state.set instanceof LinkedHashSet<Integer> linkedHashMap) {
            return linkedHashMap.getFirst();
        } else {
            return state.set.iterator().next();
        }
    }
}
