package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPOutputStream;

/**
 * This benchmark is meant to show how allocation rates impact performance on the JVM.
 *
 * <h3>Important:</h3>
 * Run all benchmarks with as many threads as you have cores! Otherwise, GC related work might be performed concurrently,
 * by CPU cores not being used by the benchmark threads directly, and therefore skew the results.
 *
 * <h3>How to run/interpret these benchmarks</h3>
 * 
 * Run & compare any of these:
 * <ol>
 *     <li>
 *         {@link #consumeCpuAndChurn(ConsumeCpuAndChurn)} and
 *         {@link #consumeCpuNoChurn(ConsumeCpuNoChurn)}.
 *         <br>
 *         This shows how one and the same <code>gc.alloc.rate.norm</code> can result in very different runtimes
 *         that depend on the amount of non-GC-related work done per allocation. In particular, you can see that the
 *         cost of churn is dwarfed by the non-GC-related work for higher <code>tokens</code> values.
 *     </li>
 *
 *     <li>
 *         {@link #randomizeAndCompressIntoNewArrays(RandomizeAndCompressIntoNewArrays)}
 *         and {@link #randomizeAndCompressReuseData(RandomizeAndCompressIntoExistingArrays)}.
 *         <br>
 *         This shows how vastly different <code>gc.alloc.rate.norm</code> values can result in very similar runtimes
 *         in a slightly more realistic setting than the example above.
 *     </li>
 * </ol>
 */
@Fork(value = 3)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
public class CostOfAllocRateNormBenchmark {

    @State(Scope.Benchmark)
    public static class RandomizeAndCompressIntoNewArrays {
        @Param("10000")
        int batchSize;
    }

    @Benchmark
    public byte[] randomizeAndCompressIntoNewArrays(RandomizeAndCompressIntoNewArrays state) throws IOException {
        var data = randomBytes(new byte[state.batchSize]);

        var bytesOut = new ByteArrayOutputStream(data.length);
        var gzipOut = new GZIPOutputStream(bytesOut);
        gzipOut.write(data);
        gzipOut.close();
        return bytesOut.toByteArray();
    }

    @State(Scope.Thread)
    public static class RandomizeAndCompressIntoExistingArrays {
        @Param("10000")
        int batchSize;

        byte[] data;
        byte[] res;
        CustomByteArrayOutputStream bytesOut;

        @Setup
        public void setup() {
            data = new byte[batchSize];
            res = new byte[batchSize];
            bytesOut = new CustomByteArrayOutputStream(batchSize);
        }
    }

    @Benchmark
    public byte[] randomizeAndCompressReuseData(RandomizeAndCompressIntoExistingArrays state) throws IOException {
        var data = randomBytes(state.data);

        var bytesOut = state.bytesOut;
        bytesOut.reset();

        var gzipOut = new GZIPOutputStream(bytesOut);
        gzipOut.write(data);
        gzipOut.close();
        return bytesOut.copyInto(state.res);
    }

    @State(Scope.Benchmark)
    public static class ConsumeCpuAndChurn {
        @Param(value = { "500", "1000", "5000" })
        int tokens;

        @Param("10000")
        int batchSize;
    }

    @Benchmark
    public byte[] consumeCpuAndChurn(ConsumeCpuAndChurn state) {
        Blackhole.consumeCPU(state.tokens);
        return new byte[state.batchSize];
    }

    @Benchmark
    public byte[] consumeCpuNoChurn(ConsumeCpuNoChurn state) {
        Blackhole.consumeCPU(state.tokens);
        return state.res;
    }

    @State(Scope.Thread)
    public static class ConsumeCpuNoChurn extends ConsumeCpuAndChurn {
        byte[] res;

        @Setup
        public void setup() {
            res = new byte[batchSize];
        }
    }

    static class CustomByteArrayOutputStream extends ByteArrayOutputStream {
        public CustomByteArrayOutputStream(int size) {
            super(size);
        }

        byte[] copyInto(byte[] dst) {
            System.arraycopy(this.buf, 0, dst, 0, Math.min(dst.length, count));
            return dst;
        }
    }

    private static byte[] randomBytes(byte[] bytes) {
        var rng = ThreadLocalRandom.current();

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ('a' + rng.nextInt(26));
        }

        return bytes;
    }
}
