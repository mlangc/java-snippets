package at.mlangc.art.of.multiprocessor.programming.ch11;

import net.jqwik.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static at.mlangc.art.of.multiprocessor.programming.ConcurrencyHelpers.runSilently;
import static org.assertj.core.api.Assertions.assertThat;

class IntStackTest {
    enum StackImpl {
        CONCURRENT(ConcurrentIntStack::new),
        CONCURRENT_WITH_LOCK_BASED_EXCHANGER(() -> new ConcurrentExchangerEnhancedStack(new LockBasedIntExchanger())),
        CONCURRENT_WITH_LOCK_FREE_EXCHANGER(() -> new ConcurrentExchangerEnhancedStack(new LockFreeIntExchanger())),
        CONCURRENT_WITH_ELIMINATION_ARRAY(() -> new ConcurrentExchangerEnhancedStack(new EliminationArrayIntExchanger(
                Runtime.getRuntime().availableProcessors() / 2,
                LockFreeIntExchanger::new,
                p -> new EliminationArrayIntExchanger.RangePolicy.Adaptive(1.1f, p)))),
        CONCURRENT_WITH_JDK_EXCHANGER(() -> new ConcurrentExchangerEnhancedStack(new JavaUtilConcurrentIntExchanger()));

        final Supplier<IntStack> ctor;

        StackImpl(Supplier<IntStack> ctor) {
            this.ctor = ctor;
        }
    }

    sealed interface Operation {

    }

    record Push(int x) implements Operation {}

    record Pop() implements Operation {}

    @Provide
    static Arbitrary<List<Operation>> arbitraryOperations() {
        return arbitraryOperation().list().ofMinSize(1).ofMaxSize(10);
    }

    private static Arbitrary<Operation> arbitraryOperation() {
        var arbPush = Arbitraries.integers().between(0, 9).map(Push::new);
        return Arbitraries.oneOf(Arbitraries.just(new Pop()), arbPush);
    }

    @Property
    void implsShouldBeConsistentWithArrayList(
            @ForAll StackImpl impl,
            @ForAll("arbitraryOperations") List<Operation> operations) {
        var arrList = new ArrayList<Integer>();
        var intStack = impl.ctor.get();

        for (Operation operation : operations) {
            if (operation instanceof Pop) {
                var expected = arrList.isEmpty() ? null : arrList.removeLast();
                var res = intStack.tryPop();
                assertThat(res).isEqualTo(expected);
            } else if (operation instanceof Push(int x)) {
                arrList.addLast(x);
                intStack.push(x);
            }
        }
    }

    @ParameterizedTest
    @EnumSource
    void pushingAndPoppingConcurrentlyShouldWork(StackImpl impl) {
        final var pushesPerJob = 100_000;
        final var concurrentPushers = 5;
        final var concurrentPoppers = 5;
        final var pushesTotal = pushesPerJob * concurrentPushers;
        final var intStack = impl.ctor.get();

        CountDownLatch pushersDone = new CountDownLatch(concurrentPushers);
        IntFunction<Runnable> newPusher = id -> () -> {
            for (int x = id; x < pushesTotal; x += concurrentPushers) {
                intStack.push(x);
            }

            pushersDone.countDown();
        };

        Supplier<BitSet> popper = () -> {
            BitSet seen = new BitSet(pushesTotal);
            var failedPops = 0;

            IntConsumer checkAndProcessPopped = popped -> {
                assertThat(seen.get(popped)).as("popped=%s", popped).isFalse();
                seen.set(popped);
            };

            Integer popped;
            do {
                if ((popped = intStack.tryPop()) != null) {
                    checkAndProcessPopped.accept(popped);
                } else {
                    failedPops++;
                }
            } while (failedPops < 10);

            runSilently(pushersDone::await);
            while ((popped = intStack.tryPop()) != null) {
                checkAndProcessPopped.accept(popped);
            }

            return seen;
        };

        var pushJobs = IntStream.range(0, concurrentPushers)
                .mapToObj(newPusher)
                .map(CompletableFuture::runAsync)
                .toList();

        var popJobs = IntStream.range(0, concurrentPoppers)
                .mapToObj(ignore -> CompletableFuture.supplyAsync(popper))
                .toList();

        assertThat(pushJobs).allSatisfy(job -> assertThat(job).succeedsWithin(1, TimeUnit.SECONDS));
        assertThat(popJobs).allSatisfy(job -> assertThat(job).succeedsWithin(1, TimeUnit.SECONDS));

        var seen = popJobs.getFirst().join();
        for (int i = 1; i < popJobs.size(); i++) {
            var seenByJob = popJobs.get(i).join();
            assertThat(seen.intersects(seenByJob)).isFalse();
            seen.or(seenByJob);
        }

        assertThat(seen.cardinality()).isEqualTo(pushesTotal);
        assertThat(seen.previousSetBit(Integer.MAX_VALUE)).isEqualTo(pushesTotal - 1);
    }
}