package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class SlottedCounterImplsBenchmark {
    @Param(value = "8")
    private int numSlots;

    @Param()
    private CounterType counterType;

    public enum CounterType {
        LOCK_BASED(SlottedCounterImpls.SlottedCounterUsingRentrantLock::new),
        ATOMIC_GUARD_BASED(SlottedCounterImpls.SlottedCounterUsingAtomicGuard::new),
        ATOMIC_ARRAY_NO_PADDING(numSlots -> new SlottedCounterImpls.SlottedCounterUsingAtomicArray(numSlots, 1)),
        ATOMIC_ARRAY_PADDED(numSlots -> new SlottedCounterImpls.SlottedCounterUsingAtomicArray(numSlots, 8));

        final IntFunction<SlottedCounterImpls.SlottedCounter> ctor;

        CounterType(IntFunction<SlottedCounterImpls.SlottedCounter> ctor) {
            this.ctor = ctor;
        }
    }

    private SlottedCounterImpls.SlottedCounter slottedCounter;

    @Setup
    public void setup() {
        slottedCounter = counterType.ctor.apply(numSlots);
    }

    @Benchmark
    public long[] slottedCounterUsingAtomicGuard() {
        slottedCounter.increment();
        return slottedCounter.counts();
    }
}
