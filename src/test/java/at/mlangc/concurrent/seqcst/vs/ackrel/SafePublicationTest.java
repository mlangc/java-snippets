package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.assertj.core.api.Assertions.assertThat;

class SafePublicationTest {
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final int[] randomValues = ThreadLocalRandom.current().ints(8 * 1024 * 1024, 1, Integer.MAX_VALUE).toArray();

    static class SomeClass {
        int value;
    }

    @ParameterizedTest
    @EnumSource
    void objectShouldBePublishedByGetAckOnStoreRel(MemoryOrdering memoryOrdering) throws InterruptedException {
        var objects = new AtomicReferenceArray<SomeClass>(1024);

        var producer = CompletableFuture.runAsync(() -> {
            var rng = ThreadLocalRandom.current();
            while (!stop.getOpaque()) {
                for (int i = 0; i < objects.length(); i++) {
                    var obj = new SomeClass();
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
