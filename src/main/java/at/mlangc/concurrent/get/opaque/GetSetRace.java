package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

class GetSetRace {
    public static void main() {
        final var memoryOrdering = MemoryOrdering.OPAQUE;
        final var iterations = 100_000;

        record Result(int r1, int r2) { }
        var results = new HashMap<Result, Integer>();

        for (int i = 0; i < iterations; i++) {
            final AtomicInteger x = new AtomicInteger();
            final AtomicInteger y = new AtomicInteger();

            var job1 = CompletableFuture.supplyAsync(() -> {
                var r1 = memoryOrdering.get(y);
                memoryOrdering.set(x, r1);
                return r1;
            });

            var job2 = CompletableFuture.supplyAsync(() -> {
                var r2 = memoryOrdering.get(x);
                memoryOrdering.set(y, 42);
                return r2;
            });

            results.merge(new Result(job1.join(), job2.join()), 1, Integer::sum);
        }

        out.printf("Results: %s%n", results);
    }

}
