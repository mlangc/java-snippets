import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.lang.System.out;

void main() {
    var executor = new Executor() {
        final Executor impl = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());

        @Override
        public void execute(Runnable command) {
            out.printf("execute(%s)%n", command);
            impl.execute(command);
        }
    };

    Runnable taskT = () -> out.println("T done");

    Supplier<CompletableFuture<Void>> taskS = () ->
            CompletableFuture.runAsync(taskT, executor).thenRun(() -> out.println("S done"));

    CompletableFuture.supplyAsync(taskS, executor)
            .thenCompose(f -> f) // Flattens Future<Future<...>> into  Future<...>
            .join();
}