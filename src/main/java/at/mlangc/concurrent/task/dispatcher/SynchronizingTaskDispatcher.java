package at.mlangc.concurrent.task.dispatcher;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * An async task dispatcher that enforces two constraints:
 *
 * <ol>
 *   <li><b>Global concurrency cap:</b> At most {@code maxTasksInFlight} tasks are scheduled to execute concurrently.
 *       If the cap is reached, the dispatching thread blocks until a slot becomes available,
 *       providing backpressure to the caller.</li>
 *   <li><b>Per-key serialization:</b> Tasks declared with overlapping synchronizer keys are
 *       executed in dispatch order — a task waits for all previously dispatched tasks that share
 *       any of its synchronizers to complete (successfully or with an error) before it starts. Tasks with no common synchronizers
 *       may run in parallel, subject to the global cap.</li>
 * </ol>
 *
 * <h4>Attention: dispatching from within a dispatched task is unsafe</h4>
 * This dispatcher is <b>non-reentrant</b>. A task that dispatches another task into the
 * same dispatcher from within its own body can <b>deadlock</b>:
 * <ul>
 *   <li>if the inner task shares a synchronizer with the outer task and the outer task
 *       <b>waits</b> on the inner result — the inner task is serialized behind the outer,
 *       which cannot complete until it returns; or</li>
 *   <li>if the global concurrency cap is already reached — the inner dispatch blocks waiting
 *       for a slot that only the (still-running) outer task can release, regardless of keys.</li>
 * </ul>
 *
 * @param <S> the type used to identify synchronizer keys
 */
public class SynchronizingTaskDispatcher<S> {
    private final Map<S, CompletableFuture<?>> futureChains = new HashMap<>();
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
            CompletableFuture<T> newChain;
            try {
                var waitFor = CompletableFuture.allOf(
                        synchronizers.stream()
                                .map(futureChains::get)
                                .filter(Objects::nonNull)
                                .toArray(CompletableFuture<?>[]::new));

                newChain = waitFor.exceptionally(_ -> null).thenCompose(_ -> {
                    try {
                        return task.execute();
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });

                synchronizers.forEach(s -> futureChains.put(s, newChain));
            } catch (Exception e) {
                releaseTaskInFlightAssumeLocked();
                throw e;
            }


            newChain.whenComplete((_, _) -> {
                lock.lock();
                try {
                    releaseTaskInFlightAssumeLocked();

                    synchronizers.forEach(s -> {
                        var chain = futureChains.get(s);
                        if (chain != null && chain.isDone()) {
                            futureChains.remove(s);
                        }
                    });
                } finally {
                    lock.unlock();
                }
            });

            return newChain;
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

    public CompletableFuture<Void> dispatch(Set<S> synchronizers, Runnable task) {
        return dispatchAsync(synchronizers, AsyncTask.of(task));
    }

    public <T> CompletableFuture<T> dispatchAsync(S synchronizer, AsyncTask<T> task) {
        return dispatchAsync(Set.of(synchronizer), task);
    }

    public <T> CompletableFuture<T> dispatch(S synchronizer, Supplier<T> task) {
        return dispatchAsync(synchronizer, AsyncTask.of(task));
    }

    public CompletableFuture<Void> dispatch(S synchronizer, Runnable task) {
        return dispatchAsync(synchronizer, AsyncTask.of(task));
    }

    public <T> CompletableFuture<T> dispatchAsync(AsyncTask<T> task) {
        return dispatchAsync(Set.of(), task);
    }

    public <T> CompletableFuture<T> dispatch(Supplier<T> task) {
        return dispatchAsync(AsyncTask.of(task));
    }

    public CompletableFuture<Void> dispatch(Runnable task) {
        return dispatchAsync(AsyncTask.of(task));
    }

    @Override
    public String toString() {
        return "SynchronizingTaskDispatcher{" +
               "maxTasksInFlight=" + maxTasksInFlight +
               '}';
    }
}
