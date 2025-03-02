package at.mlangc.vthreads.deadlock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VthreadsDeadlockDemo1 {
    public static void main() {
        new VthreadsDeadlockDemo1(true, availableProcessors() + 1, 100).run();
    }

    final Lock lock = new ReentrantLock();
    final ExecutorService executor;
    final int numThreads;
    final int sleepMillis;

    VthreadsDeadlockDemo1(boolean useVthreads, int numThreads, int sleepMillis) {
        this.numThreads = numThreads;
        this.sleepMillis = sleepMillis;

        this.executor = useVthreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newThreadPerTaskExecutor(Thread.ofPlatform().daemon().factory());
    }

    void run() {
        out.printf("Running demo with %s cores%n", Runtime.getRuntime().availableProcessors());

        var blockingJob = CompletableFuture.runAsync(this::runBlockingTask, executor);
        var syncJobs = IntStream.range(1, numThreads)
                .mapToObj(id -> CompletableFuture.runAsync(new SynchronizingTask(id), executor))
                .toArray(CompletableFuture<?>[]::new);

        blockingJob.join();
        CompletableFuture.allOf(syncJobs).join();
        out.printf("[%s] All done%n", Thread.currentThread());
    }

    void runBlockingTask() {
        lock.lock();
        try {
            out.printf("[%s] Started blocking task...%n", Thread.currentThread());
            Thread.sleep(sleepMillis);
            out.printf("[%s] IO done%n", Thread.currentThread());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    class SynchronizingTask implements Runnable {
        final int id;

        SynchronizingTask(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            out.printf("[%s] Started synchronizing task %s%n", Thread.currentThread(), id);
            synchronized (this) {
                lock.lock();
                try {
                    out.printf("[%s] Running synchronizing task %s%n", Thread.currentThread(), id);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
