package at.mlangc.concurrent.seqcst.vs.ackrel;

import at.mlangc.concurrent.MemoryOrdering;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
//@Fork(value = 1, jvmArgsAppend = {"-XX:CompileCommand=print,at.mlangc.concurrent.seqcst.vs.ackrel.MemoryOrderingBenchmark::get", "-XX:CompileCommand=dontinline,at.mlangc.benchmarks.ArrayListIterationBenchmark::addNotInlined", "-XX:+PrintFlagsFinal"})
@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class MemoryOrderingBenchmark {
    private final AtomicInteger atomic = new AtomicInteger();

    @Param
    private MemoryOrdering memoryOrdering;

    @Benchmark
    public void set() {
        memoryOrdering.set(atomic, 0xbabe);
    }

    @Benchmark
    public int get() {
        return memoryOrdering.get(atomic) & 0xc0fee;
    }
}
