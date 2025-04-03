package at.mlangc.art.of.multiprocessor.programming.ch9;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    @Test
    void addingAndRemovingConcurrentlyWorks() {
        var linkedInts = new ConcurrentSortedLinkedInts();

        final var elems = 10_000;
        Runnable addEven = () -> {
            for (int x = 0; x < elems; x += 2) {
                assertThat(linkedInts.add(x)).isTrue();
                assertThat(linkedInts.add(x)).isFalse();
            }
        };

        Runnable addOdd = () -> {
            for (int x = 1; x < elems; x += 2) {
                assertThat(linkedInts.add(x)).isTrue();
                assertThat(linkedInts.add(x)).isFalse();
            }
        };

        Runnable removeEven = () -> {
            for (int x = 0; x < elems; x += 2) {
                assertThat(linkedInts.remove(x)).isTrue();
                assertThat(linkedInts.remove(x)).isFalse();
            }
        };

        Runnable removeOdd = () -> {
            for (int x = 1; x < elems; x += 2) {
                assertThat(linkedInts.remove(x)).isTrue();
                assertThat(linkedInts.remove(x)).isFalse();
            }
        };

        var addEvenJob = CompletableFuture.runAsync(addEven);
        var addOddJob = CompletableFuture.runAsync(addOdd);
        assertThat(addEvenJob).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(addOddJob).succeedsWithin(1, TimeUnit.SECONDS);

        var removeEvenJob = CompletableFuture.runAsync(removeEven);
        var removeOddJob = CompletableFuture.runAsync(removeOdd);
        assertThat(removeEvenJob).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(removeOddJob).succeedsWithin(1, TimeUnit.SECONDS);
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