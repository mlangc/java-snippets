package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.assertj.core.api.Assertions.assertThat;

class SafePublicationTest {
    private final AtomicBoolean stop = new AtomicBoolean(false);

    static class SomeClass {
        int value;
    }

    @RepeatedTest(100)
    @EnabledOnOs(architectures = "x86_64")
    void brokenHandOverShouldWorkOnX86() {
        var holder = new Object() {
            SomeClass toBeHandedOver;
        };

        var consumer = CompletableFuture.runAsync(() -> {
            while (holder.toBeHandedOver == null) {
                Thread.onSpinWait();
            }

            assertThat(holder.toBeHandedOver.value).isPositive();
        });

        var producer = CompletableFuture.runAsync(() -> {
            var obj = new SomeClass();
            obj.value = expensiveOperationReturningPositiveNumber();
            holder.toBeHandedOver = obj;
        });

        assertThat(producer).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(consumer).succeedsWithin(1, TimeUnit.SECONDS);
    }

    private static int expensiveOperationReturningPositiveNumber() {
        return Math.max(1, UUID.randomUUID().hashCode());
    }

    @ParameterizedTest
    @EnumSource
    void objectShouldBePublishedByGetAckOnStoreRel(MemoryOrdering memoryOrdering) throws InterruptedException {
        var objects = new AtomicReferenceArray<SomeClass>(1024);
        var randomValues = ThreadLocalRandom.current().ints(8 * 1024 * 1024, 1, Integer.MAX_VALUE).toArray();

        var producer = CompletableFuture.runAsync(() -> {
            var rng = ThreadLocalRandom.current();
            while (!stop.getOpaque()) {
                for (int i = 0; i < objects.length(); i++) {
                    var obj = new SomeClass();

                    // Note: We index into a big array of ints to trigger cache misses. CPUs with a weak ordering model like
                    // ARM will then sometimes reorder the store to obj.value with the store to the objects array, unless storeRelease
                    // is used explicitly. If this happens, readers in another thread can observe value == 0.
                    obj.value = randomValues[rng.nextInt(randomValues.length)];
                    memoryOrdering.set(objects, i, obj);
                }
            }
        });

        var consumer = CompletableFuture.runAsync(() -> {
            while (!stop.getOpaque()) {
                for (int i = 0; i < objects.length(); i++) {
                    var obj = memoryOrdering.get(objects, i);
                    if (obj != null) {
                        assertThat(obj.value).isPositive();
                    }
                }
            }
        });

        Thread.sleep(500);
        stop.setOpaque(true);

        assertThat(producer).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(consumer).succeedsWithin(1, TimeUnit.SECONDS);
    }

    @AfterEach
    void afterEach() {
        stop.setOpaque(true);
    }
}
