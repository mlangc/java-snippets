void main() {
    var executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());
    Runnable taskT = () -> { };
    Runnable taskS = () -> CompletableFuture.runAsync(taskT, executor).join();
    CompletableFuture.runAsync(taskS, executor).join();
}