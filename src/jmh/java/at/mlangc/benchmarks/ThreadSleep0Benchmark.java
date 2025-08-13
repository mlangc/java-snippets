package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class ThreadSleep0Benchmark {
    @Param("85")
    private int cpuTokens;

    @Benchmark
    public void burnCpu() {
        Blackhole.consumeCPU(cpuTokens);
    }

    @Benchmark
    public void burnCpuAndSleep0() throws InterruptedException {
        Thread.sleep(0);
        Blackhole.consumeCPU(cpuTokens);
    }
}
