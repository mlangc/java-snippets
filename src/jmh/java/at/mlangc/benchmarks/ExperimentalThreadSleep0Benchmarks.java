package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class ExperimentalThreadSleep0Benchmarks {
    @State(Scope.Thread)
    public static class MostlyZeroBits {
        final BitSet bits;
        int pos;

        public MostlyZeroBits() {
            final var numBits = 1_000_000;
            final var bitsOn = 1000;

            bits = new BitSet(numBits);
            var rng = new Random(313);
            for (int i = 0; i < bitsOn; i++) {
                int bit;
                while (bits.get(bit = rng.nextInt(numBits)));
                bits.set(bit);
            }
        }

        boolean next() {
            var bit = bits.get(pos++);

            if (pos == bits.length()) {
                pos = 0;
            }

            return bit;
        }
    }


    @State(Scope.Thread)
    public static class BytesWithRng {
        final byte[] data = new byte[128];
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
    }


    @State(Scope.Benchmark)
    public static class CpuTokensConfig {
        @Param({"85"})
        private int cpuTokens;
    }

    @Benchmark
    public void sleep0() throws InterruptedException {
        Thread.sleep(0);
    }

    @Benchmark
    public void tokensOnly(CpuTokensConfig tokens) {
        Blackhole.consumeCPU(tokens.cpuTokens);
    }

    @Benchmark
    public void sleep0AndConsumeCpuTokens(CpuTokensConfig tokens) throws InterruptedException {
        Thread.sleep(0);
        Blackhole.consumeCPU(tokens.cpuTokens);
    }

    @Benchmark
    public void sleepAlmostAlways0(MostlyZeroBits bits) throws InterruptedException {
        Thread.sleep(bits.next() ? 1 : 0);
    }

    @Benchmark
    public void sleepAlmostAlways0WithFastPath(MostlyZeroBits bits) throws InterruptedException {
        if (bits.next()) {
            Thread.sleep(0);
        }
    }

    @Benchmark
    public void noop() {

    }

    @Benchmark
    public byte[] randBytes(BytesWithRng bytesWithRng) {
        bytesWithRng.rng.nextBytes(bytesWithRng.data);
        return bytesWithRng.data;
    }
}
