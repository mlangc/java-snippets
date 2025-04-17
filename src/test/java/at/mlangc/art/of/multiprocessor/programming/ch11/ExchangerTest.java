package at.mlangc.art.of.multiprocessor.programming.ch11;

import at.mlangc.art.of.multiprocessor.programming.ch11.EliminationArrayExchanger.RangePolicy;
import at.mlangc.art.of.multiprocessor.programming.ch11.Exchanger.Exchanged;
import at.mlangc.art.of.multiprocessor.programming.ch11.Exchanger.Response;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangerTest {
    enum ExchangerImpl {
        LOCK_BASED(LockBasedExchanger::new),
        LOCK_FREE(LockFreeExchanger::new),
        ELIMINATION_ARRAY_LOCK_FREE_ADAPTIVE(maxParallelism ->
                new EliminationArrayExchanger(
                        Math.max(1, maxParallelism / 2),
                        LockFreeExchanger::new,
                        maxRange -> new RangePolicy.Adaptive(1.00f, maxRange))),
        ELIMINATION_ARRAY_LOCK_FREE_CONSTANT(maxParallelism ->
                new EliminationArrayExchanger(
                        Math.max(1, maxParallelism / 2),
                        LockFreeExchanger::new,
                        maxRange -> new RangePolicy.Constant(Math.min(maxRange, 2))));

        final IntFunction<Exchanger> ctor;

        ExchangerImpl(Supplier<Exchanger> ctor) {
            this(ignore -> ctor.get());
        }

        ExchangerImpl(IntFunction<Exchanger> ctor) {
            this.ctor = ctor;
        }
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldTimeout(ExchangerImpl impl) {
        var exchanger = impl.ctor.apply(2);
        assertThat(exchanger.exchange(42, 1, TimeUnit.NANOSECONDS)).isEqualTo(Response.TIMED_OUT);
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldExchangeBetween2Threads(ExchangerImpl impl) {
        var exchanger = impl.ctor.apply(2);
        var exchange1 = CompletableFuture.supplyAsync(() -> exchanger.exchange(1, 1, TimeUnit.MINUTES));
        var exchange2 = CompletableFuture.supplyAsync(() -> exchanger.exchange(2, 1, TimeUnit.MINUTES));

        assertThat(exchange1).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(new Exchanged(2));
        assertThat(exchange2).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(new Exchanged(1));
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldExchangeAndTimeoutBetween3Threads(ExchangerImpl impl) {
        var exchanger = impl.ctor.apply(3);

        var exchanges = IntStream.range(0, 3)
                .mapToObj(x -> CompletableFuture.supplyAsync(() -> exchanger.exchange(x, 100, TimeUnit.MILLISECONDS)))
                .toList();

        assertThat(exchanges)
                .allSatisfy(e -> assertThat(e).succeedsWithin(1, TimeUnit.SECONDS))
                .extracting(CompletableFuture::join)
                .satisfiesOnlyOnce(e -> assertThat(e).isEqualTo(Response.TIMED_OUT));
    }

    @Property
    void exchangerShouldWorkUnderHighContention(
            @ForAll ExchangerImpl impl,
            @ForAll @IntRange(min = 2, max = 8) int parallelism) {
        final var limit = 2500 * parallelism;
        final var exchanger = impl.ctor.apply(parallelism);

        AtomicInteger runningExchangers = new AtomicInteger();
        LongSupplier calcTimeoutNanos = () -> (runningExchangers.getOpaque() - 1) * TimeUnit.MILLISECONDS.toNanos(1);

        IntFunction<Supplier<BitSet>> newExchangerJob = jobId -> () -> {
            runningExchangers.incrementAndGet();
            try {
                var received = new BitSet(limit);
                var lastExchanged = new int[parallelism];
                Arrays.fill(lastExchanged, -1);

                for (int x = jobId; x < limit; x += parallelism) {
                    if (exchanger.exchange(x, calcTimeoutNanos.getAsLong(), TimeUnit.NANOSECONDS) instanceof Exchanged(Integer exchanged)) {
                        assertThat(exchanged % parallelism).isNotEqualTo(jobId);
                        var r = exchanged % parallelism;

                        assertThat(lastExchanged[r])
                                .as(() -> String.format("r=%s, lastExchanged=%s", r, Arrays.toString(lastExchanged)))
                                .isLessThan(exchanged);

                        lastExchanged[r] = exchanged;
                        received.set(exchanged);
                    }
                }

                return received;
            } finally {
                runningExchangers.decrementAndGet();
            }
        };

        var jobs = IntStream.range(0, parallelism)
                .mapToObj(newExchangerJob)
                .map(CompletableFuture::supplyAsync)
                .toList();

        assertThat(jobs)
                .as("impl=%s, parallelism=%s", impl, parallelism)
                .allSatisfy(f -> assertThat(f).succeedsWithin(1, TimeUnit.SECONDS));

        var totalReceived = jobs.getFirst().join();
        for (int i = 1; i < jobs.size(); i++) {
            var localReceived = jobs.get(i).join();
            assertThat(totalReceived.intersects(localReceived)).isFalse();
            totalReceived.or(localReceived);
        }

        assertThat(totalReceived.isEmpty()).isFalse();
        assertThat(totalReceived.cardinality() % 2).isZero();
    }
}