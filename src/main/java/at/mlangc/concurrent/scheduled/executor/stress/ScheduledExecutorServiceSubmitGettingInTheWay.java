package at.mlangc.concurrent.scheduled.executor.stress;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.formatNanos;
import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.milliSleep;
import static java.lang.System.out;

public class ScheduledExecutorServiceSubmitGettingInTheWay {
    static void main() throws ExecutionException, InterruptedException {
        try (var executor = new ForkJoinPool(2)) {
            AtomicLong lastExecution = new AtomicLong(Long.MIN_VALUE);
            AtomicInteger executionCount = new AtomicInteger();
            var t0 = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                executor.execute(() -> {
                    var t1 = System.nanoTime();
                    out.printf("Executing t%s at %s, which is %s too late%n", executionCount.getAndIncrement(), formatNanos(t1), formatNanos(t1 - t0));
                    milliSleep(200);
                    lastExecution.updateAndGet(t -> Math.max(t, System.nanoTime()));
                });
            }

            var delayMillis = 500;
            var tIdeal = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMillis);
            var schedule = executor.schedule(() -> {
                var tActual = System.nanoTime();
                var delay = tActual - tIdeal;
                var delayAfterLastExecution = tActual - lastExecution.get();
                out.printf("Running s after %s t executions, %s after the last execution and %s too late%n",
                        executionCount.get(), formatNanos(delayAfterLastExecution), formatNanos(delay));
            }, delayMillis, TimeUnit.MILLISECONDS);

            schedule.get();
        }
    }
}
