package at.mlangc.art.of.multiprocessor.programming.ch10;

import at.mlangc.art.of.multiprocessor.programming.ConcurrencyHelpers;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static at.mlangc.art.of.multiprocessor.programming.ConcurrencyHelpers.runSilently;
import static org.assertj.core.api.Assertions.assertThat;

class IntQueueTest {
    enum QueueImpl {
        LOCK_BASED(LockBasedUnboundedIntQueue::new),
        CONCURRENT(ConcurrentUnboundedIntQueue::new);

        final Supplier<IntQueue> ctor;

        QueueImpl(Supplier<IntQueue> ctor) {
            this.ctor = ctor;
        }
    }

    sealed interface Operation {

    }

    record Deq() implements Operation {

    }

    record Enc(int x) implements Operation { }

    private static Arbitrary<Operation> arbitraryOperation() {
        var arbEnc = Arbitraries.integers().between(0, 9).map(Enc::new);
        return Arbitraries.oneOf(Arbitraries.just(new Deq()), arbEnc);
    }

    @Provide
    static Arbitrary<List<Operation>> arbitraryOperations(
            @ForAll @IntRange(min = 1, max = 10) int n) {
        return arbitraryOperation().list().ofSize(n);
    }

    @Property
    void shouldBeConsistentWithArrayDequeue(
            @ForAll("arbitraryOperations") List<Operation> operations,
            @ForAll QueueImpl impl) {
        var intQueue = impl.ctor.get();
        var arrDeque = new ArrayDeque<Integer>();
        for (Operation operation : operations) {
            if (operation instanceof Deq) {
                var res = intQueue.tryDeq();
                var expected = arrDeque.pollFirst();
                assertThat(res).isEqualTo(expected);

            } else if (operation instanceof Enc(int x)) {
                intQueue.enq(x);
                arrDeque.addLast(x);
            }
        }
    }

    @ParameterizedTest
    @EnumSource
    void enqueuingAndDequeuingConcurrentlyShouldWork(QueueImpl impl) {
        var intQueue = impl.ctor.get();
        final var limit = 100_000;

        IntFunction<Runnable> enqueuerWithParity = parity -> () -> {
            for (int x = parity; x < limit; x += 2) {
                intQueue.enq(x);
            }
        };

        Supplier<BitSet> dequeuer = () -> {
            var seen = new BitSet(limit);

            var failedAttempts = 0;
            do {
                try {
                    var x = intQueue.deq();
                    assertThat(seen.get(x)).isFalse();

                    var nextSetBit = seen.nextSetBit(x);
                    if (nextSetBit > x) {
                        assertThat(nextSetBit % 2).isNotEqualTo(x % 2);
                    } else {
                        assertThat(nextSetBit).isEqualTo(-1);
                    }

                    seen.set(x);
                } catch (NoSuchElementException ignore) {
                    failedAttempts++;
                    Thread.yield();
                }
            } while (failedAttempts < 100);

            return seen;
        };

        var enqJob1 = CompletableFuture.runAsync(enqueuerWithParity.apply(0));
        var enqJob2 = CompletableFuture.runAsync(enqueuerWithParity.apply(1));
        var deqJob1 = CompletableFuture.supplyAsync(dequeuer);
        var deqJob2 = CompletableFuture.supplyAsync(dequeuer);

        assertThat(enqJob1).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(enqJob2).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(deqJob1).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(deqJob2).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(deqJob1.join().intersects(deqJob2.join())).isFalse();

        var dequeued = deqJob1.join();
        dequeued.or(deqJob2.join());
        assertThat(dequeued.cardinality()).isEqualTo(limit);
        assertThat(dequeued.previousSetBit(Integer.MAX_VALUE)).isEqualTo(limit - 1);
    }

    @ParameterizedTest
    @EnumSource
    void enqueuingAndDequeueingConcurrentlyShouldWorkInMixedScenario(QueueImpl impl) {
        final var paralleism = 4;
        final var itemsPerPhase = 1000;
        final var totalItems = itemsPerPhase * paralleism * 10;
        final var allEnqueued = new CountDownLatch(paralleism);
        final var intQueue = impl.ctor.get();

        IntFunction<Supplier<BitSet>> newJob = jobId -> () -> {
            var seen = new BitSet(totalItems);
            var x = jobId;
            while (x < totalItems) {
                for (int i = 0; i < itemsPerPhase && x < totalItems; i++) {
                    intQueue.enq(x);
                    x += paralleism;
                }

                while (true) {
                    try {
                        var y = intQueue.deq();
                        assertThat(seen.get(y)).isFalse();
                        seen.set(y);
                    } catch (NoSuchElementException ignore) {
                        break;
                    }
                }
            }

            allEnqueued.countDown();
            runSilently(allEnqueued::await);

            while (true) {
                try {
                    var y = intQueue.deq();
                    assertThat(seen.get(y)).isFalse();
                    seen.set(y);
                } catch (NoSuchElementException ignore) {
                    break;
                }
            }

            return seen;
        };

        var jobs = IntStream.range(0, paralleism)
                .mapToObj(newJob)
                .map(CompletableFuture::supplyAsync)
                .toList();

        assertThat(jobs).allSatisfy(job ->
                assertThat(job).succeedsWithin(1, TimeUnit.SECONDS));

        var seen = new BitSet(totalItems);
        for (CompletableFuture<BitSet> job : jobs) {
            var seenFromJob = job.join();
            assertThat(seen.intersects(seenFromJob)).isFalse();
            seen.or(seenFromJob);
        }

        assertThat(seen.cardinality()).isEqualTo(totalItems);
        assertThat(seen.previousSetBit(Integer.MAX_VALUE)).isEqualTo(totalItems - 1);
    }
}