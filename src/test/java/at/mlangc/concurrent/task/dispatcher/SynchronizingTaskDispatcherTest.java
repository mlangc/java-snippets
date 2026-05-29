package at.mlangc.concurrent.task.dispatcher;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


class SynchronizingTaskDispatcherTest {

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldExecuteTrivialTasks(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var f1 = dispatcher.dispatch(() -> 1);
        var f2 = dispatcher.dispatch(0, () -> 2);
        var f3 = dispatcher.dispatch(Set.of(0, 1), () -> 3);

        assertThat(f1).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(1);
        assertThat(f2).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(2);
        assertThat(f3).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldProperlySerializeTasksInSimpleExample(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var task1Counter = new MutableInt();
        var task2Counter = new MutableInt();
        var targetCount = 50_000;

        Supplier<Integer> task1 = task1Counter::getAndIncrement;
        Supplier<Integer> task2 = task2Counter::getAndIncrement;

        var task1Futures = new HashMap<Integer, CompletableFuture<Integer>>();
        var task2Futures = new HashMap<Integer, CompletableFuture<Integer>>();
        for (var i = 0; i < targetCount; i++) {
            task1Futures.put(i, dispatcher.dispatch(1, task1));
            task2Futures.put(i, dispatcher.dispatch(2, task2));
        }

        assertThat(task1Futures.entrySet())
                .allSatisfy(entry -> assertThat(entry.getValue()).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(entry.getKey()));

        assertThat(task2Futures.entrySet())
                .allSatisfy(entry -> assertThat(entry.getValue()).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(entry.getKey()));

        assertThat(task1Counter.get()).isEqualTo(targetCount);
        assertThat(task2Counter.get()).isEqualTo(targetCount);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldRespectOrderForSingleSynchronizer(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var numTasks = 10_000;
        var counter = new MutableInt();

        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numTasks; i++) {
            futures.add(
                    dispatcher.dispatch(0, () -> {
                        assertThat(counter.intValue()).isZero();
                        counter.increment();
                        assertThat(counter.intValue()).isOne();
                        return null;
                    })
            );

            futures.add(
                    dispatcher.dispatch(0, () -> {
                        assertThat(counter.intValue()).isOne();
                        counter.decrement();
                        assertThat(counter.intValue()).isZero();
                        return null;
                    })
            );
        }

        assertThat(CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))).succeedsWithin(5, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @MethodSource("synchronizingIntegerTaskDispatchers")
    void shouldProperlySerializeTasksInMoreElaborateExample(SynchronizingTaskDispatcher<Integer> dispatcher) {
        var numSynchronizers = 8;
        var setCounters = new long[1 << numSynchronizers];
        var individualCounters = new long[numSynchronizers];
        var numTasks = 100_000;
        var rng = new Random(23);

        var futures = new ArrayList<CompletableFuture<Void>>();
        for (var i = 0; i < numTasks; i++) {
            var synchronizersId = rng.nextInt(setCounters.length);
            var synchronizers = IntStream.range(0, numSynchronizers)
                    .filter(bit -> ((1 << bit) & synchronizersId) != 0)
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());

            IntFunction<Supplier<Void>> newTaskWithDelta = delta -> () -> {
                for (var s : synchronizers) {
                    assertThat(individualCounters[s]).isNotNegative();
                    individualCounters[s] += delta;
                    assertThat(individualCounters[s]).isNotNegative();
                }

                if (synchronizersId != 0) {
                    assertThat(setCounters[synchronizersId]).as("synchronizers=%s", synchronizers).isNotNegative();
                    setCounters[synchronizersId] += delta;
                    assertThat(setCounters[synchronizersId]).as("synchronizers=%s", synchronizers).isNotNegative();
                }

                return null;
            };

            var delta = rng.nextInt(10);
            futures.add(dispatcher.dispatch(synchronizers, newTaskWithDelta.apply(delta)));
            futures.add(dispatcher.dispatch(synchronizers, newTaskWithDelta.apply(-delta)));
        }

        assertThat(CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))).succeedsWithin(5, TimeUnit.SECONDS);

        assertThat(Arrays.stream(individualCounters)).allMatch(c -> c >= 0);
        assertThat(Arrays.stream(setCounters)).allMatch(c -> c >= 0);
    }

    static List<SynchronizingTaskDispatcher<Integer>> synchronizingIntegerTaskDispatchers() {
        return List.of(new SynchronizingTaskDispatcher<>(500), new SynchronizingTaskDispatcher<>(50), new SynchronizingTaskDispatcher<>(5));
    }
}