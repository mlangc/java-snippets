package at.mlangc.benchmarks;

import org.awaitility.core.ThrowingRunnable;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.*;

@Fork(value = 1)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class ExperimentalThreadSleep0ExecutorBasedBenchmark {
    public enum ExecutorConfig {
        VIRTUAL_THREAD_PER_TASK,
        WORK_STEALING_1,
        WORK_STEALING_NUM_CPUS,
        FIXED_THREAD_POOL_1,
        FIXED_THREAD_POOL_NUM_CPUS,
        COMMON_POOL
    }

    @Param("COMMON_POOL")
    private ExecutorConfig executorConfig;

    private ExecutorService executor;

    @Setup
    public void setup() {
        executor = switch (executorConfig) {
            case COMMON_POOL -> ForkJoinPool.commonPool();
            case WORK_STEALING_1 -> Executors.newWorkStealingPool(1);
            case FIXED_THREAD_POOL_1 -> Executors.newFixedThreadPool(1);
            case WORK_STEALING_NUM_CPUS -> Executors.newWorkStealingPool();
            case VIRTUAL_THREAD_PER_TASK -> Executors.newVirtualThreadPerTaskExecutor();
            case FIXED_THREAD_POOL_NUM_CPUS -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        };
    }

    @TearDown
    public void tearDown() throws TimeoutException, InterruptedException {
        if (executorConfig == ExecutorConfig.COMMON_POOL) {
            return;
        }

        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new TimeoutException();
        }
    }

    @Benchmark
    public void sleep0() {
        launchConcurrentlyAndWait(() -> Thread.sleep(0));
    }

    @Benchmark
    public void noop() {
        launchConcurrentlyAndWait(() -> { });
    }

    private void launchConcurrentlyAndWait(ThrowingRunnable runnable) {
        var nJobs = 8 * 8 * 8 * 8;
        var jobs = new CompletableFuture[nJobs];

        Runnable wrappedRunnable = () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };

        for (int i = 0; i < nJobs; i++) {
            jobs[i] = CompletableFuture.runAsync(wrappedRunnable, executor);
        }

        CompletableFuture.allOf(jobs).join();
    }
}
