import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.lang.System.out;

void main() {
    var executor = Executors.newVirtualThreadPerTaskExecutor();

    Runnable taskT = () -> out.println("T done");

    Runnable taskS = () -> {
        CompletableFuture.runAsync(taskT, executor).join();
        out.println("S done");
    };

    CompletableFuture.runAsync(taskS, executor).join();
}