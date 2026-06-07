import static java.lang.System.out;

void main() {
    var executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon().factory());
    IntFunction<Runnable> newTaskT = i -> () -> out.printf("T%s done%n", i);

    IntFunction<Runnable> newTaskS = i -> () -> {
        CompletableFuture.runAsync(newTaskT.apply(i), executor).join();
        out.printf("S%s done%n", i);
    };

    CompletableFuture.allOf(
            CompletableFuture.runAsync(newTaskS.apply(1), executor),
            CompletableFuture.runAsync(newTaskS.apply(2), executor)
    ).join();
}