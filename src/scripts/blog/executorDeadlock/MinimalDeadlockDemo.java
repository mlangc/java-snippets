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

    Runnable taskS = () -> {
        CompletableFuture.runAsync(taskT, executor).join();
        out.println("S done");
    };

    CompletableFuture.runAsync(taskS, executor).join();
}