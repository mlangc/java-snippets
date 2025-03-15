package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class SynchronizedVsReentrantLock {
    private final Lock lock = new ReentrantLock();
    private final Semaphore semaphore = new Semaphore(1);
    private int counter;
    private final AtomicInteger atomicCounter = new AtomicInteger();

    @Benchmark
    public synchronized int incrementAndGetSynchronized() {
        return ++counter;
    }

    @Benchmark
    public int incrementAndGetReentrantLock() {
        lock.lock();
        try {
            return ++counter;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    public int atomicIncrementAndGet() {
        return atomicCounter.incrementAndGet();
    }

    @Benchmark
    public int incrementAndGetSemaphore() throws InterruptedException {
        semaphore.acquire();
        try {
            return ++counter;
        } finally {
            semaphore.release();
        }
    }
}
