package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class ReleaseAcquireRace {
    final AtomicBoolean started1 = new AtomicBoolean();
    final AtomicBoolean started2 = new AtomicBoolean();

    boolean first1;
    boolean first2;

    void run1() {
        started1.setRelease(true);

        if (!started2.getAcquire()) {
            first1 = true;
        }
    }

    void run2() {
        started2.setRelease(true);

        if (!started1.getAcquire()) {
            first2 = true;
        }
    }

    public static void main(String[] args) {
        for (long tries = 1; tries < Long.MAX_VALUE; tries++) {
            var race = new ReleaseAcquireRace();

            var run1 = CompletableFuture.runAsync(race::run1);
            var run2 = CompletableFuture.runAsync(race::run2);

            run1.join();
            run2.join();

            if (race.first1 && race.first2) {
                System.out.printf("Both threads won after %s tries%n", tries);
                break;
            }
        }
    }
}
