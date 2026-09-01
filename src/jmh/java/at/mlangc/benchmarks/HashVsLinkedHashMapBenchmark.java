package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
public class HashVsLinkedHashMapBenchmark {
    public enum MapImpl {
        HASH_MAP, LINKED_HASH_MAP, CONCURRENT_HASH_MAP;

        Map<Integer, Integer> newInstance(float loadFactor, Map<Integer, Integer> entries) {
            Map<Integer, Integer> res = switch (this) {
                case HASH_MAP -> new HashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
                case LINKED_HASH_MAP -> new LinkedHashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
                case CONCURRENT_HASH_MAP -> new ConcurrentHashMap<>(calculateMapCapacity(entries.size(), loadFactor), loadFactor);
            };

            res.putAll(entries);
            return res;
        }

        static int calculateMapCapacity(int numMappings, float loadFactor) {
            return (int) Math.ceil(numMappings / (double) loadFactor);
        }
    }

    @State(Scope.Benchmark)
    public static class ReadOnlyBenchmarkState {
        @Param
        MapImpl impl;

        @Param({"0.1", "0.2", "0.4", "0.8"})
        float loadFactor;

        @Param("50000")
        int size;

        Map<Integer, Integer> map;

        @Setup
        public void setup() {
            var rng = new Random(313);
            var entries = HashMap.<Integer, Integer>newHashMap(size);

            while (entries.size() < size) {
                entries.put(rng.nextInt(), rng.nextInt());
            }

            map = impl.newInstance(loadFactor, entries);
        }
    }

    @Benchmark
    public void iterateOverAll(ReadOnlyBenchmarkState state, Blackhole bh) {
        for (var o : state.map.entrySet()) {
            bh.consume(o);
        }
    }

    @Benchmark
    public Object getFirst(ReadOnlyBenchmarkState state) {
        if (state.map instanceof LinkedHashMap<Integer, Integer> map) {
            return map.firstEntry();
        }

        return state.map.entrySet().iterator().next();
    }
}
