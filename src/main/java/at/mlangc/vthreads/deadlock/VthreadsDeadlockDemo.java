package at.mlangc.vthreads.deadlock;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VthreadsDeadlockDemo {
    public static void main() {
        new VthreadsDeadlockDemo(true, 8, 100).run();
    }

    final Lock lock = new ReentrantLock();
    final ExecutorService executor;
    final int threadsPerCpu;
    final int sleepMillis;
    final CountDownLatch ioStarted = new CountDownLatch(1);

    VthreadsDeadlockDemo(boolean useVthreads, int threadsPerCpu, int sleepMillis) {
        this.executor = useVthreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newThreadPerTaskExecutor(Thread::new);
        this.threadsPerCpu = threadsPerCpu;
        this.sleepMillis = sleepMillis;
    }

    void run() {
        var numCpus = Runtime.getRuntime().availableProcessors();
        var numTasks = numCpus * threadsPerCpu;

        var ioJob = CompletableFuture.runAsync(this::runIoTask, executor);
        var syncJobs = IntStream.range(1, numTasks)
                .mapToObj(id -> CompletableFuture.runAsync(new SynchronizingTask(id), executor))
                .toArray(CompletableFuture<?>[]::new);

        ioJob.join();
        CompletableFuture.allOf(syncJobs).join();
    }

    void runIoTask() {
        lock.lock();
        try {
            out.println("Running task 0 (IO task)");
            ioStarted.countDown();
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
            try {
                ioStarted.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(e);
            }

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
