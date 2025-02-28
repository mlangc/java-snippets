package at.mlangc.vthreads.deadlock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VthreadsDeadlockDemo2 {
    private final int sleepMillis;
    private final List<WaitingTask> waitingTasks;
    private final ExecutorService executor;
    private volatile boolean done = false;

    public static void main() {
        new VthreadsDeadlockDemo2(true, 257, 100).run();
    }

    VthreadsDeadlockDemo2(boolean useVthreads, int numThreads, int sleepMillis) {
        this.sleepMillis = sleepMillis;

        this.executor = useVthreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newThreadPerTaskExecutor(Thread.ofPlatform().daemon().factory());

        this.waitingTasks = IntStream.range(0, numThreads - 1)
                .mapToObj(ignore -> new WaitingTask())
                .toList();
    }

    void run() {
        out.printf("Running demo with %s cores%n", Runtime.getRuntime().availableProcessors());

        var notifyingJob = CompletableFuture.runAsync(new NotifyingTask(), executor);
        var waitingJobs = waitingTasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executor))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(waitingJobs).join();
        CompletableFuture.allOf(notifyingJob).join();
        out.printf("[%s] All done%n", Thread.currentThread());
    }

    class WaitingTask implements Runnable {
        @Override
        public synchronized void run() {
            try {
                while (!done) {
                    out.printf("[%s] Waiting...%n", Thread.currentThread());
                    this.wait();
                    out.printf("[%s] Done%n", Thread.currentThread());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class NotifyingTask implements Runnable {
        @Override
        public void run() {
            try {
                out.printf("[%s] Going to sleep...%n", Thread.currentThread());
                Thread.sleep(sleepMillis);
                out.printf("[%s] Done sleeping...%n", Thread.currentThread());
                done = true;

                for (WaitingTask task : waitingTasks) {
                    synchronized (task) {
                        task.notify();
                    }
                }

                out.printf("[%s] Notified%n", Thread.currentThread());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
