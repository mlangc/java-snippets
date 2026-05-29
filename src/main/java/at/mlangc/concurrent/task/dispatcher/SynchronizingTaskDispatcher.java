package at.mlangc.concurrent.task.dispatcher;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SynchronizingTaskDispatcher<S> {
    private final Map<S, CompletableFuture<?>> futureChains = new WeakHashMap<>();
    private final int maxTasksInFlight;
    private int tasksInFlight;
    private final Lock lock = new ReentrantLock();
    private final Condition tasksUnblocked = lock.newCondition();

    public SynchronizingTaskDispatcher(int maxTasksInFlight) {
        Preconditions.checkArgument(maxTasksInFlight > 0);
        this.maxTasksInFlight = maxTasksInFlight;
    }

    public <T> CompletableFuture<T> dispatchAsync(Set<S> synchronizers, AsyncTask<T> task) {
        lock.lock();
        try {
            while (tasksInFlight == maxTasksInFlight) {
                tasksUnblocked.await();
            }

            tasksInFlight++;
            var restoreNeeded = true;
            try {
                var waitFor = CompletableFuture.allOf(
                        synchronizers.stream()
                                .map(futureChains::get)
                                .filter(Objects::nonNull)
                                .toArray(CompletableFuture<?>[]::new));

                var newChain = waitFor.exceptionally(_ -> null).thenCompose(_ -> {
                    try {
                        return task.execute().whenComplete((_, _) -> {
                            lock.lock();
                            releaseTaskInFlightAssumeLocked();
                            lock.unlock();
                        });
                    } catch (Exception e) {
                        lock.lock();
                        releaseTaskInFlightAssumeLocked();
                        lock.unlock();
                        return CompletableFuture.failedFuture(e);
                    }
                });

                restoreNeeded = false;
                synchronizers.forEach(s -> futureChains.put(s, newChain));
                return newChain;
            } catch (Exception e) {
                if (restoreNeeded) {
                    releaseTaskInFlightAssumeLocked();
                }

                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } finally {
            lock.unlock();
        }
    }

    private void releaseTaskInFlightAssumeLocked() {
        if (tasksInFlight-- == maxTasksInFlight) {
            tasksUnblocked.signalAll();
        }
    }

    public <T> CompletableFuture<T> dispatch(Set<S> synchronizers, Supplier<T> task) {
        return dispatchAsync(synchronizers, AsyncTask.of(task));
    }

    public <T> CompletableFuture<T> dispatchAsync(S synchronizer, AsyncTask<T> task) {
        return dispatchAsync(Set.of(synchronizer), task);
    }

    public <T> CompletableFuture<T> dispatch(S synchronizers, Supplier<T> task) {
        return dispatchAsync(synchronizers, AsyncTask.of(task));
    }

    public <T> CompletableFuture<T> dispatchAsync(AsyncTask<T> task) {
        return dispatchAsync(Set.of(), task);
    }

    public <T> CompletableFuture<T> dispatch(Supplier<T> task) {
        return dispatchAsync(AsyncTask.of(task));
    }

    @Override
    public String toString() {
        return "SynchronizingTaskDispatcher{" +
               "maxTasksInFlight=" + maxTasksInFlight +
               '}';
    }
}
