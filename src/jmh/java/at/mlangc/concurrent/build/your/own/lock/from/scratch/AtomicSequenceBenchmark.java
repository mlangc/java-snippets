package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
        COMPARE_AND_SET_LOCK, JAVA_REENTRANT_LOCK, SYNCHRONIZED, ATOMIC_GET_AND_INCREMENT, GET_AND_SET_LOCK, REENTRANT_GET_AND_SET_LOCK,
        REENTRANT_GET_AND_SET_LOCK_WITH_BACKOFF, CLH_QUEUE_LOCK, CLH_QUEUE_WITH_HASH_MAP_LOCK, CLH_QUEUE_LOCK_FANCY;
    }

    @Param({"JAVA_REENTRANT_LOCK", "CLH_QUEUE_LOCK", "COMPARE_AND_SET_LOCK"})
    SeqImplType implType;

    AtomicSequence sequence;

    @Setup
    public void setup() {
        sequence = switch (implType) {
            case SYNCHRONIZED -> new AtomicSequence() {
                long value;

                @Override
                public synchronized long next() {
                    return value++;
                }
            };

            case ATOMIC_GET_AND_INCREMENT -> new AtomicSequence() {
                final AtomicLong value = new AtomicLong();

                @Override
                public long next() {
                    return value.getAndIncrement();
                }
            };

            case JAVA_REENTRANT_LOCK -> new SimpleLockBasedSequence(new JavaUtilConcurrentReentrantLock());
            case COMPARE_AND_SET_LOCK -> new SimpleLockBasedSequence(new CompareAndSetLock());
            case GET_AND_SET_LOCK -> new SimpleLockBasedSequence(new GetAndSetLock());
            case REENTRANT_GET_AND_SET_LOCK -> new SimpleLockBasedSequence(new ReentrantGetAndSetLock());
            case REENTRANT_GET_AND_SET_LOCK_WITH_BACKOFF -> new SimpleLockBasedSequence(new ReentrantGetAndSetLockWithBackoff(500, 10_000, TimeUnit.MICROSECONDS));
            case CLH_QUEUE_LOCK -> new SimpleLockBasedSequence(new ClhQueueLock());
            case CLH_QUEUE_WITH_HASH_MAP_LOCK -> new SimpleLockBasedSequence(new ClhQueueWithHashMapLock());
            case CLH_QUEUE_LOCK_FANCY -> new SimpleLockBasedSequence(new FancyClhQueueLock());
        };
    }

    @Benchmark
    public long getAndIncrementLocked() {
        return sequence.next();
    }
}
