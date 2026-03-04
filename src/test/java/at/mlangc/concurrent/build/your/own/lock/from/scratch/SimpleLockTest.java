package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SimpleLockTest {
    static ExecutorService executor;

    @BeforeAll
    static void beforeAll() {
        executor = Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory());
    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }

    enum LockImpl {
		JAVA_REENTRANT_LOCK(JavaUtilReentrantLock::new),
		COMPARE_AND_SET_LOCK(CompareAndSetLock::new),
		COMPARE_AND_SET_UNFAIR_QUEUE_LOCK(() -> new CompareAndSetLockQueueLock(false)),
		COMPARE_AND_SET_FAIR_QUEUE_LOCK(() -> new CompareAndSetLockQueueLock(true));

        final Supplier<SimpleLock> factory;

        LockImpl(Supplier<SimpleLock> factory) {
            this.factory = factory;
        }
    }

    @ParameterizedTest
    @EnumSource
    void shouldProtectSharedCounter(LockImpl impl) {
        var lock = impl.factory.get();

		var sharedLongs = new Object() {
			final long[] values = new long[32];

			void incrementAll() {
				for (int i = 0; i < values.length; i++) {
					values[i]++;
				}
			}
		};

		final var incrementsPerThread = 25_000;
		final var numThreads = 8;

		CompletableFuture<?>[] jobs = IntStream.range(0, numThreads)
				.mapToObj(_ -> CompletableFuture.runAsync(() -> {
							for (int i = 0; i < incrementsPerThread; i++) {
								if (lock.isReentrant() && (i & 1) != 0) {
									lock.runWithLock(() -> lock.runWithLock(sharedLongs::incrementAll));
								} else {
									lock.runWithLock(sharedLongs::incrementAll);
								}

								if (lock.hasCheckedUnlock() & (i & 0xFF) == 0xFF) {
									assertThatExceptionOfType(IllegalMonitorStateException.class).isThrownBy(lock::unlock);
								}
							}
						}, executor)
				).toArray(CompletableFuture[]::new);
		assertThat(CompletableFuture.allOf(jobs)).succeedsWithin(5, TimeUnit.SECONDS);

		var expectedValues = new long[sharedLongs.values.length];
		Arrays.fill(expectedValues, incrementsPerThread * numThreads);
		assertThat(sharedLongs.values).isEqualTo(expectedValues);
	}

	@ParameterizedTest
    @EnumSource
    void reentrantLocksShouldBeReentrantInBasicScenario(LockImpl impl) {
        var lock = impl.factory.get();
        assumeThat(lock.isReentrant()).isTrue();
        assertThatNoException().isThrownBy(() -> lock.runWithLock(() -> lock.runWithLock(() -> { })));
    }

    @ParameterizedTest
    @EnumSource
    void checkedLocksShouldBeCheckedInBasicScenario(LockImpl impl) {
        var lock = impl.factory.get();
        assumeThat(lock.hasCheckedUnlock()).isTrue();
        assertThatExceptionOfType(IllegalMonitorStateException.class).isThrownBy(lock::unlock);
    }
}