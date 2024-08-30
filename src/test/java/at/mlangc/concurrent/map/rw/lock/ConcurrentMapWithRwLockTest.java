package at.mlangc.concurrent.map.rw.lock;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentMapWithRwLockTest {
    private static final int PARALLELISM = 4;
    private static final int MAX_ELEMS = 10_000;
    private static final double SIZE_CHECK_RATIO = 0.001;

    @Test
    void sizeShouldStayConsistentWhileHoldingWriteLock() throws InterruptedException {
        new TestScenario().run();
    }

    private static class TestScenario {
        private final ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private volatile boolean stop = false;

        void run() throws InterruptedException {
            var runLoops = IntStream.range(0, PARALLELISM)
                    .mapToObj(ignore -> CompletableFuture.runAsync(this::runLoop))
                    .toArray(CompletableFuture<?>[]::new);

            Thread.sleep(5_000);
            stop = true;
            assertThat(CompletableFuture.allOf(runLoops)).succeedsWithin(5, TimeUnit.SECONDS);
            System.out.println("Map after test: size=" + concurrentMap.size() + ", data=" + concurrentMap);
        }

        void runLoop() {
            var rng = ThreadLocalRandom.current();
            while (!stop) {
                runSingleStep(rng);
            }
        }

        void runSingleStep(Random rng) {
            if (ThreadLocalRandom.current().nextDouble() < SIZE_CHECK_RATIO) {
                checkSizeConsistency();
            } else {
                randomlyModify(rng);
            }
        }

        void randomlyModify(Random rng) {
            var key = rng.nextInt(0, MAX_ELEMS);

            rwLock.readLock().lock();
            try {
                if (key % 2 == 0) {
                    concurrentMap.merge(key, 1, Integer::sum);
                } else {
                    concurrentMap.computeIfPresent(key - 1, (ignore, v) -> v - 1 == 0 ? null : v - 1);
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }

        void checkSizeConsistency() {
            rwLock.writeLock().lock();
            try {
                var size1 = concurrentMap.size();
                var size2 = concurrentMap.size();

                var size3 = 0;
                for (Integer ignore : concurrentMap.keySet()) {
                    size3++;
                }

                assertThat(size1).isEqualTo(size2).isEqualTo(size3);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
}
