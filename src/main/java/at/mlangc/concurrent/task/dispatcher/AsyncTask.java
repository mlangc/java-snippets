package at.mlangc.concurrent.task.dispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public interface AsyncTask<T> {
    CompletableFuture<T> execute();

    static AsyncTask<Void> of(Runnable runnable, Executor executor) {
        return () -> CompletableFuture.runAsync(runnable, executor);
    }

    static AsyncTask<Void> of(Runnable runnable) {
        return () -> CompletableFuture.runAsync(runnable);
    }

    static <T> AsyncTask<T> of(Supplier<T> supplier, Executor executor) {
        return () -> CompletableFuture.supplyAsync(supplier, executor);
    }

    static <T> AsyncTask<T> of(Supplier<T> supplier) {
        return () -> CompletableFuture.supplyAsync(supplier);
    }
}
