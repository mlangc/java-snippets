import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.lang.System.out;

void main() {
    var executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());
    Runnable taskT = () -> out.println("T done");

    Supplier<CompletableFuture<Void>> taskS = () ->
            CompletableFuture.runAsync(taskT, executor).thenRun(() -> out.println("S done"));

    CompletableFuture.supplyAsync(taskS, executor)
            .thenCompose(f -> f) // Flattens Future<Future<...>> into Future<...>
            .join();
}