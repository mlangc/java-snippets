package at.mlangc.concurrent.scheduled.executor.stress;

import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.formatNanos;
import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.milliSleep;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledExecutorServiceSubmitGettingInTheWay {
    private static final Logger LOG = LogManager.getLogger(ScheduledExecutorServiceSubmitGettingInTheWay.class);

    static void main() throws ExecutionException, InterruptedException {
        try (var executor = new ForkJoinPool(1)) {
            AtomicLong lastExecution = new AtomicLong(Long.MIN_VALUE);
            AtomicInteger executionCount = new AtomicInteger();
            var t0 = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                executor.execute(() -> {
                    var t1 = System.nanoTime();
                    LOG.printf(Level.INFO, "Executing t%s at %s, which is %s too late", executionCount.getAndIncrement(), formatNanos(t1), formatNanos(t1 - t0));
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
                LOG.printf(Level.INFO, "Running s after %s t executions, %s after the last execution and %s too late",
                        executionCount.get(), formatNanos(delayAfterLastExecution), formatNanos(delay));
            }, delayMillis, TimeUnit.MILLISECONDS);

            schedule.get();
        }
    }
}
