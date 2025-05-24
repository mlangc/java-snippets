package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@State(Scope.Benchmark)
//@Fork(value = 1, jvmArgsAppend = {"-XX:CompileCommand=print,at.mlangc.concurrent.seqcst.vs.ackrel.PetersonLock::lock", "-XX:CompileCommand=dontinline,at.mlangc.benchmarks.ArrayListIterationBenchmark::addNotInlined", "-XX:+PrintFlagsFinal"})
@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 25, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class IndexedLockBenchmark {
    private static final int NUM_THREADS = 2;

    public enum LockType {
        BAKERY(() -> new ReentrantBakeryLock(NUM_THREADS, MemoryOrdering.VOLATILE)),
        DEKKERS(DekkersLock::new),
        PETERSON(ReentrantPetersonLock::new),
        GET_AND_SET(ReentrantGetAndSetLock::new),
        PETERSON_VOLATILE(() -> new TweakablePetersonLock(MemoryOrdering.VOLATILE, false)),
        PETERSON_ACK_REL_FENCE(() -> new TweakablePetersonLock(MemoryOrdering.ACQUIRE_RELEASE, true)),
        BAKERY_VOLATILE(() -> new TweakableBakeryLock(2, MemoryOrdering.VOLATILE, false)),
        BAKERY_ACK_REL_FENCE(() -> new TweakableBakeryLock(2, MemoryOrdering.ACQUIRE_RELEASE, true)),
        COMPARE_AND_SET_ACK_REL(() -> new CompareAndSetLock(MemoryOrdering.ACQUIRE_RELEASE)),
        COMPARE_AND_SET_VOLATILE(() -> new CompareAndSetLock(MemoryOrdering.VOLATILE));

        final Supplier<IndexedLock> ctor;

        LockType(Supplier<IndexedLock> ctor) {
            this.ctor = ctor;
        }
    }

    @Param({"BAKERY_VOLATILE", "BAKERY_ACK_REL_FENCE", "PETERSON_VOLATILE", "PETERSON_ACK_REL_FENCE", "COMPARE_AND_SET_ACK_REL", "COMPARE_AND_SET_VOLATILE"})
    private LockType lockType;

    @Param("50000")
    private int incrementsPerThread;

    private final ExecutorService executor = ThreadIndex.newIndexedFixedThreadPool(NUM_THREADS);
    private int sharedCounter;
    private IndexedLock lock;

    @Setup
    public void setup() {
        lock = lockType.ctor.get();
    }

    @TearDown
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out");
        }
    }

    @Benchmark
    public int incrementContendedSharedCounter() {
        var futures = new ArrayList<CompletableFuture<Void>>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(CompletableFuture.runAsync(() -> incrementWithLock(incrementsPerThread), executor));
        }

        for (CompletableFuture<Void> future : futures) {
            future.join();
        }

        return sharedCounter;
    }

    private void incrementWithLock(int increments) {
        for (int i = 0; i < increments; i++) {
            lock.lock();
            sharedCounter++;
            lock.unlock();
        }
    }
}
