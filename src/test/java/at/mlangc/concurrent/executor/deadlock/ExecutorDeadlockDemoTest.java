package at.mlangc.concurrent.executor.deadlock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ExecutorDeadlockDemoTest {
    @Test
    void demonstrateDeadlockWithSingleThreadedExecutorUsingRecursiveTask() throws InterruptedException {
        new DoWithExecutor(Executors.newSingleThreadExecutor()).run(executor -> {
            Runnable runAsyncNoopThatBlocks = () -> futureGetUnchecked(CompletableFuture.runAsync(() -> { }, executor));
            var deadlocked = CompletableFuture.runAsync(runAsyncNoopThatBlocks, executor);
            assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> deadlocked.get(250, TimeUnit.MILLISECONDS));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 7, 8, 16})
    void demonstrateDeadlockWithMultiThreadedExecutorUsingRecursiveTasks(int nThreads) throws InterruptedException {
        new DoWithExecutor(Executors.newFixedThreadPool(nThreads)).run(executor -> {
            Runnable runAsyncOpsThatBlock = () -> { };
            for (int i = 0; i < nThreads; i++) {
                var runAsyncOpsThatBlockFinal = runAsyncOpsThatBlock;
                runAsyncOpsThatBlock = () -> futureGetUnchecked(CompletableFuture.runAsync(runAsyncOpsThatBlockFinal, executor));
            }

            var deadlocked = CompletableFuture.runAsync(runAsyncOpsThatBlock, executor);
            assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> deadlocked.get(250, TimeUnit.MILLISECONDS));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 7, 8, 16})
    void demonstrateDeadlockWithMultiThreadedExecutorUsingLatch(int nThreads) throws InterruptedException {
        new DoWithExecutor(Executors.newFixedThreadPool(nThreads)).run(executor -> {
            var latch = new CountDownLatch(nThreads + 1);
            var deadlocked = CompletableFuture.allOf(
                    IntStream.range(0, nThreads + 1)
                            .mapToObj(_ -> CompletableFuture.runAsync(() -> {
                                latch.countDown();
                                awaitUnchecked(latch);
                            }, executor)).toArray(CompletableFuture<?>[]::new)
            );

            assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> deadlocked.get(250, TimeUnit.MILLISECONDS));
        });
    }

    @ParameterizedTest
    @CsvSource({"1,1,11", "1,1,11", "2,1,11"})
    void demonstrateDeadlockWithHttpClient(int maxInFlight, int httpExecutorThreads, int testRequests) throws InterruptedException {
        new DoWithExecutor(Executors.newFixedThreadPool(httpExecutorThreads)).run(httpClientExecutor -> {
            var wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
            wireMockServer.start();
            var permitsForRequestsInFlight = new Semaphore(maxInFlight);

            try (var httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build()) {
                wireMockServer.stubFor(get("/test").willReturn(ok()));
                URI testUri = URI.create(wireMockServer.url("/test"));

                UncheckedSupplier<CompletableFuture<HttpResponse<Void>>> runTestRequest =
                        () -> CompletableFuture.runAsync((UncheckedRunnable) (permitsForRequestsInFlight::acquire))
                                .thenCompose(_ ->
                                        httpClient.sendAsync(HttpRequest.newBuilder(testUri).build(), HttpResponse.BodyHandlers.discarding()))
                                .whenComplete((_, _) -> permitsForRequestsInFlight.release());

                var runningTestRequest = CompletableFuture.allOf(
                        IntStream.range(0, testRequests)
                                .mapToObj(_ -> runTestRequest.get())
                                .toArray(CompletableFuture<?>[]::new));

                assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> runningTestRequest.get(100, TimeUnit.MILLISECONDS));
            } finally {
                permitsForRequestsInFlight.release(testRequests);
                wireMockServer.shutdownServer();
            }
        });
    }

    record DoWithExecutor(ExecutorService executor) {
        void run(ThrowingConsumer<ExecutorService> op) throws InterruptedException {
            try {
                op.accept(executor);
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    static void awaitUnchecked(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    static <T> T futureGetUnchecked(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface UncheckedRunnable extends Runnable {
        void runWithUncheckedException() throws Exception;

        default void run() {
            try {
                runWithUncheckedException();
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FunctionalInterface
    interface UncheckedSupplier<T> extends Supplier<T> {
        T getWithCheckedException() throws Exception;

        default T get() {
            try {
                return getWithCheckedException();
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
