package at.mlangc.vthreads.deadlock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VthreadsDeadlockDemo {
    public static void main() {
        new VthreadsDeadlockDemo(true, 9, 100).run();
    }

    final Lock lock = new ReentrantLock();
    final ExecutorService executor;
    final int numThreads;
    final int sleepMillis;

    VthreadsDeadlockDemo(boolean useVthreads, int numThreads, int sleepMillis) {
        this.executor = useVthreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newThreadPerTaskExecutor(Thread::new);
        this.numThreads = numThreads;
        this.sleepMillis = sleepMillis;
    }

    void run() {
        var ioJob = CompletableFuture.runAsync(this::runIoTask, executor);
        var syncJobs = IntStream.range(1, numThreads)
                .mapToObj(id -> CompletableFuture.runAsync(new SynchronizingTask(id), executor))
                .toArray(CompletableFuture<?>[]::new);

        ioJob.join();
        CompletableFuture.allOf(syncJobs).join();
    }

    void runIoTask() {
        lock.lock();
        try {
            out.println("Running task 0 (IO task)");
            Thread.sleep(sleepMillis);
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
            synchronized (this) {
                lock.lock();
                try {
                    out.printf("Running task %s%n", id);
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
