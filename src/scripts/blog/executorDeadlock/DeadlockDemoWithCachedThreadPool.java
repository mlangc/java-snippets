void main() {
    var executor = Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory());
    var threadsToBlock = 10_000;

    Runnable chainOfDeath = () -> System.out.println("done");
    for (int i = 0; i < threadsToBlock; i++) {
        var chainOfDeathFinal = chainOfDeath;
        chainOfDeath = () -> CompletableFuture.runAsync(chainOfDeathFinal, executor).join();
    }

    CompletableFuture.runAsync(chainOfDeath, executor).join();
}