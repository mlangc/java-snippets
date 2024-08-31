package at.mlangc.concurrent.map.rw.lock;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static java.lang.System.out;
import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentMapWithRwLockTest {
    private static final int PARALLELISM = 2;
    private static final int MAX_ENTRIES = 10_000;
    private static final double CONSISTENCY_CHECK_RATIO = 0.01;
    private static final int RUN_MILLIS = 5000;

    @Test
    void sizeShouldStayConsistentWhileHoldingWriteLock() throws InterruptedException {
        new TestScenarioWithRwLock().run();
    }

    @Test
    void sizeShouldStayConsistentWhileHavingPermitInLockFreeImpl() throws InterruptedException {
        new LockFreeTestScenario().run();
    }

    private static abstract class TestScenarioTemplate {
        private final ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        private volatile boolean stop = false;
        private final LongAdder modifications = new LongAdder();
        private final LongAdder consistencyChecks = new LongAdder();

        abstract void acquireModificationPermit();
        abstract void releaseModificationPermit();
        abstract void acquireConsistencyCheckPermit();
        abstract void releaseConsistencyCheckPermit();

        void run() throws InterruptedException {
            var runLoops = IntStream.range(0, PARALLELISM)
                    .mapToObj(ignore -> CompletableFuture.runAsync(this::runLoop))
                    .toArray(CompletableFuture<?>[]::new);

            Thread.sleep(RUN_MILLIS);
            stop = true;
            assertThat(CompletableFuture.allOf(runLoops)).succeedsWithin(5, TimeUnit.SECONDS);

            out.printf("Map size=%s, some data=%s%n", concurrentMap.size(), concurrentMap.entrySet().stream().limit(3).toList());

            var actualConsistencyCheckRatio = consistencyChecks.doubleValue() / modifications.doubleValue();
            out.printf(
                    "modifications=%s, consistencyChecks=%s, configuredConsistencyCheckRatio=%s, actualConsistencyCheckRatio=%s, consistencyCheckRatioDeviation=%s",
                    modifications.longValue(), consistencyChecks.longValue(),
                    CONSISTENCY_CHECK_RATIO, actualConsistencyCheckRatio, actualConsistencyCheckRatio / CONSISTENCY_CHECK_RATIO);

        }

        void runLoop() {
            var rng = ThreadLocalRandom.current();
            while (!stop) {
                runSingleStep(rng);
            }
        }

        void runSingleStep(Random rng) {
            if (ThreadLocalRandom.current().nextDouble() < CONSISTENCY_CHECK_RATIO) {
                checkSizeConsistency();
            } else {
                randomlyModify(rng);
            }
        }

        void randomlyModify(Random rng) {
            var key = rng.nextInt(0, MAX_ENTRIES);

            acquireModificationPermit();
            try {
                if (key % 2 == 0) {
                    concurrentMap.merge(key, 1, Integer::sum);
                } else {
                    concurrentMap.computeIfPresent(key - 1, (ignore, v) -> v - 1 == 0 ? null : v - 1);
                }
            } finally {
                releaseModificationPermit();
            }

            modifications.increment();
        }

        void checkSizeConsistency() {
            acquireConsistencyCheckPermit();
            try {
                var size1 = concurrentMap.size();
                var size2 = concurrentMap.size();

                var size3 = 0;
                for (Integer ignore : concurrentMap.keySet()) {
                    size3++;
                }

                assertThat(size1).isEqualTo(size2).isEqualTo(size3);
            } finally {
                releaseConsistencyCheckPermit();
            }

            consistencyChecks.increment();
        }
    }

    private static class TestScenarioWithRwLock extends TestScenarioTemplate {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        @Override
        void acquireModificationPermit() {
            rwLock.readLock().lock();
        }

        @Override
        void releaseModificationPermit() {
            rwLock.readLock().unlock();
        }

        @Override
        void acquireConsistencyCheckPermit() {
            rwLock.writeLock().lock();
        }

        @Override
        void releaseConsistencyCheckPermit() {
            rwLock.writeLock().unlock();
        }
    }

    private static class LockFreeTestScenario extends TestScenarioTemplate {
        static final VarHandle MAP_STATE_VAR_HANDLE;

        static {
            try {
                MAP_STATE_VAR_HANDLE = MethodHandles.lookup().findVarHandle(LockFreeTestScenario.class, "mapState", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError("Cannot create var handle for mapState", e);
            }
        }

        // Used via VAR_HANDLE
        @SuppressWarnings("unused")
        private int mapState;

        @Override
        void acquireModificationPermit() {
            while (true) {
                var current = (int) MAP_STATE_VAR_HANDLE.getOpaque(this);
                if (current >= 0 && MAP_STATE_VAR_HANDLE.weakCompareAndSetPlain(this, current, current + 1)) {
                    break;
                }
            }
        }

        @Override
        void releaseModificationPermit() {
            while (true) {
                var current = (int) MAP_STATE_VAR_HANDLE.getOpaque(this);
                if (MAP_STATE_VAR_HANDLE.weakCompareAndSetPlain(this, current, current - 1)) {
                    break;
                }
            }
        }

        @Override
        void acquireConsistencyCheckPermit() {
            while (true) {
                if (MAP_STATE_VAR_HANDLE.weakCompareAndSetPlain(this, 0, -1)) {
                    break;
                }
            }
        }

        @Override
        void releaseConsistencyCheckPermit() {
            while (true) {
                if (MAP_STATE_VAR_HANDLE.weakCompareAndSetPlain(this, -1, 0)) {
                    break;
                }
            }
        }
    }
}
