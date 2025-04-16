package at.mlangc.art.of.multiprocessor.programming.ch11;

import at.mlangc.art.of.multiprocessor.programming.ch11.Exchanger.Exchanged;
import at.mlangc.art.of.multiprocessor.programming.ch11.Exchanger.Response;
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
        LOCK_BASED(LockBasedExchanger::new);

        final Supplier<Exchanger> ctor;

        ExchangerImpl(Supplier<Exchanger> ctor) {
            this.ctor = ctor;
        }
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldTimeout(ExchangerImpl impl) {
        var exchanger = impl.ctor.get();
        assertThat(exchanger.exchange(42, 1, TimeUnit.NANOSECONDS)).isEqualTo(Response.TIMED_OUT);
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldExchangeBetween2Threads(ExchangerImpl impl) {
        var exchanger = impl.ctor.get();
        var exchange1 = CompletableFuture.supplyAsync(() -> exchanger.exchange(1, 1, TimeUnit.MINUTES));
        var exchange2 = CompletableFuture.supplyAsync(() -> exchanger.exchange(2, 1, TimeUnit.MINUTES));

        assertThat(exchange1).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(new Exchanged(2));
        assertThat(exchange2).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(new Exchanged(1));
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldExchangeAndTimeoutBetween3Threads(ExchangerImpl impl) {
        var exchanger = impl.ctor.get();

        var exchanges = IntStream.range(0, 3)
                .mapToObj(x -> CompletableFuture.supplyAsync(() -> exchanger.exchange(x, 100, TimeUnit.MILLISECONDS)))
                .toList();

        assertThat(exchanges)
                .allSatisfy(e -> assertThat(e).succeedsWithin(1, TimeUnit.SECONDS))
                .extracting(CompletableFuture::join)
                .satisfiesOnlyOnce(e -> assertThat(e).isEqualTo(Response.TIMED_OUT));
    }

    @ParameterizedTest
    @EnumSource
    void exchangerShouldWorkUnderHighContention(ExchangerImpl impl) {
        final var parallelism = Runtime.getRuntime().availableProcessors();
        final var limit = 5000 * parallelism;
        final var exchanger = impl.ctor.get();

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
                .allSatisfy(f -> assertThat(f).succeedsWithin(1, TimeUnit.SECONDS));

        var totalReceived = jobs.getFirst().join();
        for (int i = 1; i < jobs.size(); i++) {
            var localReceived = jobs.get(i).join();
            assertThat(totalReceived.intersects(localReceived)).isFalse();
            totalReceived.or(localReceived);
        }
    }
}