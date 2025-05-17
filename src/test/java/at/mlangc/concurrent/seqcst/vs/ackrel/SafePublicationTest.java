package at.mlangc.concurrent.seqcst.vs.ackrel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SafePublicationTest {
    private final AtomicBoolean stop = new AtomicBoolean(false);

    @ParameterizedTest
    @EnumSource
    void objectShouldBePublishedByGetAckOnStoreRel(MemoryOrdering memoryOrdering) {
        List<CompletableFuture<Void>> jobs = new ArrayList<>();

        for (int i = 0; i < 10_000; i++) {
            AtomicBoolean ready = new AtomicBoolean(false);
            var result = new LinkedList<Integer>();
            var somethingElse = new ArrayList<Integer>();
            Runnable publishResult = () -> {
                for (int j = 0; j < 256; j++) {
                    result.add(42);
                }

                memoryOrdering.set(ready, true);

                for (int j = 0; j < 10; j++) {
                    somethingElse.add(23);
                }
            };

            Runnable consumeAndCheckResult = () -> {
                while (!stop.getOpaque() && !memoryOrdering.get(ready)) {
                    Thread.onSpinWait();
                }

                assertThat(result)
                        .hasSize(256)
                        .allSatisfy(elem -> assertThat(elem).isEqualTo(42));

                assertThat(somethingElse).isNotNull();
            };

            jobs.add(CompletableFuture.runAsync(publishResult));
            jobs.add(CompletableFuture.runAsync(consumeAndCheckResult));
        }

        assertThat(jobs).allSatisfy(job -> assertThat(job).succeedsWithin(1, TimeUnit.SECONDS));
    }

    @AfterEach
    void afterEach() {
        stop.setOpaque(true);
    }
}
