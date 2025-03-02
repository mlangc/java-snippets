package at.mlangc.vthreads.deadlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VthreadsDeadlockDemo3 {
    final Lock lock = new ReentrantLock();
    final ExecutorService executor;
    final int numThreads;
    final int sleepMillis;

    private final ServerSocket serverSocket;

    public static void main() throws IOException, InterruptedException {
        new VthreadsDeadlockDemo3(true, availableProcessors() + 1, 100).run();
    }

    VthreadsDeadlockDemo3(boolean useVthreads, int numThreads, int sleepMillis) throws IOException {
        this.sleepMillis = sleepMillis;
        this.serverSocket = new ServerSocket(0);

        this.numThreads = numThreads;
        this.executor = useVthreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newThreadPerTaskExecutor(Thread.ofPlatform().daemon().factory());
    }

    void run() throws IOException, InterruptedException {
        out.printf("Running demo with %s cores%n%n", availableProcessors());

        try (var clientSocket = new Socket(this.serverSocket.getInetAddress(), this.serverSocket.getLocalPort())) {
            var waitJob = CompletableFuture.runAsync(this::waitForMessage, executor);
            var syncJobs = IntStream.range(1, numThreads)
                    .mapToObj(id -> CompletableFuture.runAsync(new SynchronizingTask(id), executor))
                    .toArray(CompletableFuture<?>[]::new);

            out.printf("[%s] Going to sleep...%n", Thread.currentThread());
            Thread.sleep(sleepMillis);
            clientSocket.getOutputStream().write("hello\n".getBytes(StandardCharsets.UTF_8));
            out.printf("[%s] Message sent%n", Thread.currentThread());

            waitJob.join();
            CompletableFuture.allOf(syncJobs).join();
            out.printf("[%s] All done%n", Thread.currentThread());
        }
    }

    void waitForMessage() {
        lock.lock();
        try (var connection = serverSocket.accept()) {
            out.printf("[%s] Waiting for message...%n", Thread.currentThread());
            var in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            out.printf("[%s] Message '%s' received%n", Thread.currentThread(), in.readLine());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
