package at.mlangc.art.of.multiprocessor.programming.ch9;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SortedLinkedIntsTest {
    sealed interface Operation {
        boolean apply(SortedLinkedInts linkedInts);
    }

    record Add(int x) implements Operation {
        @Override
        public boolean apply(SortedLinkedInts linkedInts) {
            return linkedInts.add(x);
        }
    }

    record Remove(int x) implements Operation {
        @Override
        public boolean apply(SortedLinkedInts linkedInts) {
            return linkedInts.remove(x);
        }
    }

    record Contains(int x) implements Operation {
        @Override
        public boolean apply(SortedLinkedInts linkedInts) {
            return linkedInts.contains(x);
        }
    }

    @Test
    void concurrentSortedLinkedIntsShouldWorkInExample1() {
        var linkedInts = new ConcurrentSortedLinkedInts();
        assertThat(linkedInts.add(0)).isTrue();
        assertThat(linkedInts.add(0)).isFalse();
        assertThat(linkedInts.add(1)).isTrue();
        assertThat(linkedInts.add(0)).isFalse();
    }

    @Test
    void concurrentSortedLinkedIntsShouldWorkInExample2() {
        var linkedInts = new ConcurrentSortedLinkedInts();
        assertThat(linkedInts.remove(0)).isFalse();
        assertThat(linkedInts.remove(0)).isFalse();
    }

    @Test
    void concurrentSortedLinkedIntsShouldWorkInExample3() {
        var linkedInts = new ConcurrentSortedLinkedInts();
        assertThat(linkedInts.add(0)).isTrue();
        assertThat(linkedInts.add(0)).isFalse();
        assertThat(linkedInts.remove(0)).isTrue();
        assertThat(linkedInts.remove(0)).isFalse();
    }

    @Property
    void implementationsShouldBeConsistentForSingleThread(@ForAll("arbitraryOperations") List<Operation> operations) {
        var treeSet = new SynchronizedTreeSet();
        var concurrentInts = new ConcurrentSortedLinkedInts();

        for (int i = 0; i < operations.size(); i++) {
            var operation = operations.get(i);
            var resTs = operation.apply(treeSet);
            var resCi = operation.apply(concurrentInts);
            assertThat(resTs).as("ops=%s", operations.subList(0, i + 1)).isEqualTo(resCi);
        }
    }

    @Property
    void addingAndRemovingOddEvenConcurrentlyWorks(@ForAll @IntRange(min = 1, max = 1000) int numElems) {
        var linkedInts = new ConcurrentSortedLinkedInts();

        IntFunction<Runnable> addWithParity = parity -> () -> {
            IntFunction<Supplier<String>> desc = x -> () -> String.format("numElems=%s, ctx=add, parity=%s, x=%s", numElems, parity, x);

            for (int x = parity; x < numElems; x += 2) {
                assertThat(linkedInts.add(x)).as(desc.apply(x)).isTrue();
                assertThat(linkedInts.add(x)).as(desc.apply(x)).isFalse();
                assertThat(linkedInts.contains(x)).as(desc.apply(x)).isTrue();
            }
        };

        IntFunction<Runnable> removeWithParity = parity -> () -> {
            IntFunction<Supplier<String>> desc = x -> () -> String.format("numElems=%s, ctx=remove, parity=%s, x=%s", numElems, parity, x);

            for (int x = parity; x < numElems; x += 2) {
                assertThat(linkedInts.remove(x)).as(desc.apply(x)).isTrue();
                assertThat(linkedInts.remove(x)).as(desc.apply(x)).isFalse();
                assertThat(linkedInts.contains(x)).as(desc.apply(x)).isFalse();
            }
        };

        var addEven = addWithParity.apply(0);
        var addOdd = addWithParity.apply(1);

        var removeEven = removeWithParity.apply(0);
        var removeOdd = removeWithParity.apply(1);

        var addEvenJob = CompletableFuture.runAsync(addEven);
        var addOddJob = CompletableFuture.runAsync(addOdd);
        assertThat(addEvenJob).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(addOddJob).succeedsWithin(1, TimeUnit.SECONDS);

        var removeEvenJob = CompletableFuture.runAsync(removeEven);
        var removeOddJob = CompletableFuture.runAsync(removeOdd);
        assertThat(removeEvenJob).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(removeOddJob).succeedsWithin(1, TimeUnit.SECONDS);
    }

    record AddRemoveInPairsConcurrencyTestCase(List<List<Integer>> concurrentOps) {}

    @Property
    void addingAndRemovingInPairsConcurrentlyShouldResultInEmptyCollection(
            @ForAll("arbitraryAddRemoveInPairsConcurrencyTestCase") AddRemoveInPairsConcurrencyTestCase testCase) {
        var linkedInts = new ConcurrentSortedLinkedInts();

        Function<List<Integer>, Runnable> runAddRemove = pairs -> () -> {
            var seen = new HashSet<Integer>();
            for (Integer x : pairs) {
                if (seen.add(x)) {
                    assertThat(linkedInts.add(x)).isTrue();
                } else {
                    assertThat(linkedInts.remove(x)).isTrue();
                }
            }
        };

        var jobs = testCase.concurrentOps.stream()
                .map(pairs -> CompletableFuture.runAsync(runAddRemove.apply(pairs)))
                .toList();

        assertThat(jobs)
                .as(testCase::toString)
                .allSatisfy(job -> assertThat(job).succeedsWithin(1, TimeUnit.SECONDS));

        var allInts = testCase.concurrentOps.stream().flatMap(List::stream).distinct().toList();
        assertThat(allInts)
                .as(testCase::toString)
                .allSatisfy(x -> assertThat(linkedInts.contains(x)).isFalse());
    }

    @Provide
    static Arbitrary<AddRemoveInPairsConcurrencyTestCase> arbitraryAddRemoveInPairsConcurrencyTestCase(
            @ForAll @IntRange(min = 1, max = 8) int parallelism,
            @ForAll @IntRange(min = 1, max = 100) int pairsPerJob) {
        var numParis = parallelism * pairsPerJob;
        var arbAllPairs = Arbitraries.integers().set().ofSize(numParis);

        return arbAllPairs.flatMap(allPairs -> {
            var groupedPairs = new ArrayList<List<Integer>>(parallelism);
            groupedPairs.add(new ArrayList<>(pairsPerJob * 2));
            for (Integer p : allPairs) {
                if (groupedPairs.getLast().size() == 2 * pairsPerJob) {
                    groupedPairs.add(new ArrayList<>(pairsPerJob * 2));
                }

                groupedPairs.getLast().add(p);
                groupedPairs.getLast().add(p);
            }

            return Combinators.combine(groupedPairs.stream().map(Arbitraries::shuffle).toList())
                    .as(AddRemoveInPairsConcurrencyTestCase::new);
        });
    }

    @Provide
    static Arbitrary<List<Operation>> arbitraryOperations(@ForAll @IntRange(min = 0, max = 25) int n) {
        return arbitraryOperation().list().ofSize(n);
    }

    static Arbitrary<Operation> arbitraryOperation() {
        var arbX = Arbitraries.integers();
        var arbAdd = arbX.map(Add::new);
        var arbRemove = arbX.map(Remove::new);
        var arbContains = arbX.map(Contains::new);
        return Arbitraries.oneOf(arbAdd, arbRemove, arbContains);
    }

    static class SynchronizedTreeSet implements SortedLinkedInts {
        private final TreeSet<Integer> ints = new TreeSet<>();

        @Override
        public synchronized boolean add(int x) {
            return ints.add(x);
        }

        @Override
        public synchronized boolean remove(int x) {
            return ints.remove(x);
        }

        @Override
        public synchronized boolean contains(int x) {
            return ints.contains(x);
        }
    }
}