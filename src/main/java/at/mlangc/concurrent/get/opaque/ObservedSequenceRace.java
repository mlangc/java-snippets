package at.mlangc.concurrent.get.opaque;

import at.mlangc.concurrent.MemoryOrdering;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class ObservedSequenceRace implements AutoCloseable {
    private final MemoryOrdering memoryOrdering;
    private final ExecutorService virtualThreadsExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService raceExecutorService = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
    private final AtomicInteger[] work = new AtomicInteger[1_000_000];

    public static void main(String[] args) throws InterruptedException, TimeoutException {
        try (var race = new ObservedSequenceRace(MemoryOrdering.PLAIN)) {
            var results = race.run().stream().map(RaceResult::toString).sorted().toList();

            out.printf("Observed results: %n");
            for (var result : results) {
                out.printf("  %s%n", result);
            }
        }
    }

    Set<RaceResult> run() throws InterruptedException {
        var parallelism = 100;
        var atomicsPerThread = work.length / parallelism;
        var done = new AtomicBoolean();
        var finalResult = new HashSet<RaceResult>();

        IntFunction<CompletableFuture<Set<RaceResult>>> startRacerJob = id ->
                CompletableFuture.supplyAsync(() -> {
                    var observed = new HashSet<RaceResult>();
                    var rng = ThreadLocalRandom.current();

                    while (!done.getOpaque()) {
                        var iA = rng.nextInt(atomicsPerThread);
                        var iB = rng.nextInt(atomicsPerThread);
                        if (iA == iB) continue;

                        var idxA = iA * parallelism + id;
                        var idxB = iB * parallelism + id;

                        observed.add(new GetSetRace(work[idxA], work[idxB]).run());
                    }

                    return observed;
                }, virtualThreadsExecutor);

        var racerJobs = IntStream.range(0, parallelism)
                .mapToObj(startRacerJob)
                .toList();

        Thread.sleep(1000);
        done.setOpaque(true);

        for (CompletableFuture<Set<RaceResult>> racerJob : racerJobs) {
            finalResult.addAll(racerJob.join());
        }

        return finalResult;
    }

    public ObservedSequenceRace(MemoryOrdering memoryOrdering) {
        this.memoryOrdering = memoryOrdering;
        Arrays.setAll(work, ignore -> new AtomicInteger());
    }


    @Override
    public void close() throws TimeoutException, InterruptedException {
        virtualThreadsExecutor.shutdown();
        raceExecutorService.shutdown();

        if (!virtualThreadsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new TimeoutException("Timed out");
        }

        if (!raceExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
            throw new TimeoutException("Timed out");
        }
    }

    record RaceResult(int a1, int b1, int a2, int b2, int a3, int b3) {

    }

    class GetSetRace {
        private final AtomicInteger a;
        private final AtomicInteger b;

        GetSetRace(AtomicInteger a, AtomicInteger b) {
            this.a = a;
            this.b = b;
        }

        RaceResult run() {
            a.setPlain(10);
            b.setPlain(20);

            var setJob = CompletableFuture.runAsync(() -> {
                memoryOrdering.set(a, 11);
                memoryOrdering.set(a, 12);
                memoryOrdering.set(a, 13);

                memoryOrdering.set(b, 21);
                memoryOrdering.set(b, 22);
                memoryOrdering.set(b, 23);
            }, raceExecutorService);

            var getJob = CompletableFuture.supplyAsync(() ->
                            new RaceResult(
                                    memoryOrdering.get(a), memoryOrdering.get(b),
                                    memoryOrdering.get(a), memoryOrdering.get(b),
                                    memoryOrdering.get(a), memoryOrdering.get(b))
                    , raceExecutorService);

            setJob.join();
            return getJob.join();
        }
    }
}
