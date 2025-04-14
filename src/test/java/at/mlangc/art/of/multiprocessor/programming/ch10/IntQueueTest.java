package at.mlangc.art.of.multiprocessor.programming.ch10;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class IntQueueTest {
    enum QueueImpl {
        LOCK_BASED(LockBasedUnboundedIntQueue::new);

        final Supplier<IntQueue> ctor;

        QueueImpl(Supplier<IntQueue> ctor) {
            this.ctor = ctor;
        }
    }

    @ParameterizedTest
    @EnumSource
    void enqueuingAndDequeuingConcurrentlyShouldWork(QueueImpl impl) {
        var intQueue = impl.ctor.get();
        final var limit = 25_000;

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

        var dequeued = deqJob1.join();
        dequeued.or(deqJob2.join());
        assertThat(dequeued.cardinality()).isEqualTo(limit);
        assertThat(dequeued.previousSetBit(Integer.MAX_VALUE)).isEqualTo(limit - 1);
    }

}