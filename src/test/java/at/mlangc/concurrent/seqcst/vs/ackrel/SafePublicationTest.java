package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

class SafePublicationTest {
    private final AtomicBoolean stop = new AtomicBoolean(false);

    static class SomeClass {
        int value;
    }

    @Test
    @EnabledOnOs(architectures = "x86_64")
    void unsafeHandOverShouldWorkOnX86() {
        var holder = new Object() {
            SomeClass published;
        };

        var failures = new LongAdder();
        for (int repetition = 0; repetition < 1000; repetition++) {
            var consumer = CompletableFuture.runAsync(() -> {
                while (holder.published == null) {
                    Thread.onSpinWait();
                }

                if (holder.published.value == 0) {
                    failures.increment();
                }
            });

            var producer = CompletableFuture.runAsync(() -> {
                var obj = new SomeClass();

                // We make it deliberately much more expensive to set obj.value than
                // to set holder.toBeHandedOver. This is meant to entice CPUs with
                // weaker ordering guaranties, to execute these statements out of order.
                obj.value = ThreadLocalRandom.current().nextInt(1, 1000);
                holder.published = obj;
            });

            assertThat(producer).succeedsWithin(1, TimeUnit.SECONDS);
            assertThat(consumer).succeedsWithin(1, TimeUnit.SECONDS);
            holder.published = null;
        }

        assertThat(failures).isZero();
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "PLAIN" })
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
