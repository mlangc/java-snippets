package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class AtomicSequenceBenchmark {
    interface AtomicSequence {
        long next();
    }

    static class SimpleLockBasedSequence implements AtomicSequence {
        final SimpleLock lock;
        long value;

        SimpleLockBasedSequence(SimpleLock lock) {
            this.lock = lock;
        }

        @Override
        public long next() {
            try {
                lock.lock();
                return value++;
            } finally {
                lock.unlock();
            }
        }
    }

    public enum SeqImplType {
		USING_BROKEN_LOCK
    }

    @Param({"USING_BROKEN_LOCK"})
    SeqImplType implType;

    AtomicSequence sequence;

    @Setup
    public void setup() {
        sequence = switch (implType) {
			case USING_BROKEN_LOCK -> new SimpleLockBasedSequence(new BrokenNoopLock());
        };
    }

    @Benchmark
    public long getAndIncrementLocked() {
        return sequence.next();
    }
}
