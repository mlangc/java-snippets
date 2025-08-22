package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class ObservedSequenceRace implements AutoCloseable {
    private final ExecutorService virtualThreadsExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger[] work = new AtomicInteger[5_000_000];

    public static void main(String[] args) throws InterruptedException, TimeoutException {
        try (var race = new ObservedSequenceRace()) {
            var results = race.run().entrySet().stream()
                    .map(e ->
                            e.getKey() + "@" + orderingString(e.getValue()) + ":" + markerString(e.getKey()))
                    .sorted()
                    .toList();

            out.printf("Observed results: %n");
            for (var result : results) {
                out.printf("  %s%n", result);
            }
        }
    }

    private static String markerString(RaceResult result) {
        if (result.spectacular()) return "S";
        else if (result.interesting()) return "i";
        else return "b";
    }

    private static CharSequence orderingString(Set<MemoryOrdering> orderings) {
        StringBuilder sb = new StringBuilder(MemoryOrdering.values().length);
        for (MemoryOrdering ordering : MemoryOrdering.values()) {
            if (orderings.contains(ordering)) {
                sb.append(ordering.name().charAt(0));
            } else {
                sb.append('x');
            }
        }
        return sb;
    }

    Map<RaceResult, Set<MemoryOrdering>> run() throws InterruptedException {
        var parallelism = 100;
        var atomicsPerThread = work.length / parallelism;
        var done = new AtomicBoolean();
        var finalResult = new HashMap<RaceResult, Set<MemoryOrdering>>();

        IntFunction<CompletableFuture<Map<RaceResult, Set<MemoryOrdering>>>> startRacerJob = id ->
                CompletableFuture.supplyAsync(() -> {
                    var observed = new HashMap<RaceResult, Set<MemoryOrdering>>();
                    var rng = ThreadLocalRandom.current();

                    Supplier<MemoryOrdering> randomMemoryOrdering = () ->
                            MemoryOrdering.values()[rng.nextInt(MemoryOrdering.values().length)];

                    Function<RaceResult, Set<MemoryOrdering>> newEmptyEnumSet = ignore -> EnumSet.noneOf(MemoryOrdering.class);

                    while (!done.getOpaque()) {
                        var iA = rng.nextInt(atomicsPerThread);
                        var iB = rng.nextInt(atomicsPerThread);
                        if (iA == iB) continue;

                        var idxA = iA * parallelism + id;
                        var idxB = iB * parallelism + id;

                        var memoryOrdering = randomMemoryOrdering.get();
                        var result = new SetGetRace(memoryOrdering, work[idxA], work[idxB]).run();
                        observed.computeIfAbsent(result, newEmptyEnumSet).add(memoryOrdering);
                    }

                    return observed;
                }, virtualThreadsExecutor);

        var racerJobs = IntStream.range(0, parallelism)
                .mapToObj(startRacerJob)
                .toList();

        Thread.sleep(5_000);
        done.setOpaque(true);

        for (var racerJob : racerJobs) {
            racerJob.join().forEach((res, ordering) ->
                    finalResult.merge(res, ordering, (current, addition) -> {
                        current.addAll(addition);
                        return current;
                    }));
        }

        return finalResult;
    }

    public ObservedSequenceRace() {
        Arrays.setAll(work, ignore -> new AtomicInteger());
    }


    @Override
    public void close() throws TimeoutException, InterruptedException {
        virtualThreadsExecutor.shutdown();

        if (!virtualThreadsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new TimeoutException("Timed out");
        }
    }

    static class SetGetRace {
        private final MemoryOrdering memoryOrdering;
        private final AtomicInteger a;
        private final AtomicInteger b;

        SetGetRace(MemoryOrdering memoryOrdering, AtomicInteger a, AtomicInteger b) {
            this.memoryOrdering = memoryOrdering;
            this.a = a;
            this.b = b;
        }

        RaceResult run() {
            a.setPlain(0);
            b.setPlain(0);

            var setJob = CompletableFuture.runAsync(() -> {
                memoryOrdering.set(a, 1);
                memoryOrdering.set(a, 2);

                memoryOrdering.set(b, 1);
                memoryOrdering.set(b, 2);
            });

            var getJob = CompletableFuture.supplyAsync(() -> {
                // Note that a is written before b,
                // but b is read before a.
                var b1 = memoryOrdering.get(b);
                var a1 = memoryOrdering.get(a);
                var b2 = memoryOrdering.get(b);
                var a2 = memoryOrdering.get(a);

                return new RaceResult(b1, a1, b2, a2);
            });

            setJob.join();
            return getJob.join();
        }
    }

    record RaceResult(int b1, int a1, int b2, int a2) {
        boolean interesting() {
            // Only possible if reads or writes have been reordered.
            return b1 > a1 || b2 > a2;
        }

        boolean spectacular() {
            // Only possible if reads or writes to the same variable
            // have been reordered.
            return a1 > a2 || b1 > b2;
        }
    }
}
