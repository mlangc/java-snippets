package at.mlangc.vthreads.threadlocal;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class VirtualThreadsThreadLocalDemo {
    private final int numTasks;
    private final ExecutorService executor;
    private final ThreadLocal<Object> threadLocal = ThreadLocal.withInitial(Object::new);
    private final Set<Object> objectsCreated = Collections.newSetFromMap(new ConcurrentHashMap<>());


    public VirtualThreadsThreadLocalDemo(int numTasks, boolean useVirtualThreads) {
        this.numTasks = numTasks;

        this.executor = useVirtualThreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory());
    }

    public static void main() {
        new VirtualThreadsThreadLocalDemo(100_000, true).run();
    }

    void run() {
        var jobs = IntStream.range(0, numTasks)
                .mapToObj(ignore -> CompletableFuture.runAsync(this::runTask, executor))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(jobs).join();
        out.printf("Created %s objects after running %s tasks", objectsCreated.size(), numTasks);
    }

    private void runTask() {
        objectsCreated.add(threadLocal.get());
    }
}
