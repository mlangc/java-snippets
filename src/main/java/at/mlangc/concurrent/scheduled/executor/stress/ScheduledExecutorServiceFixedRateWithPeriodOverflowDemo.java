package at.mlangc.concurrent.scheduled.executor.stress;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.*;
import static java.lang.System.out;

public class ScheduledExecutorServiceFixedRateWithPeriodOverflowDemo {
    static void main() {
        var executor = ForkJoinPool.commonPool();
        var periodMillis = 100;
        var sleepMillis = List.of(300, 10, 10, 10, 10);
        var sleepMillisIndex = new MutableInt();
        var scheduleCounter = new MutableInt();
        var t0 = System.nanoTime();

        var schedule = executor.scheduleAtFixedRate(() -> {
            var actualNanos = System.nanoTime();
            var scheduleNumber = scheduleCounter.getAndIncrement();
            var idealNanos = t0 + scheduleNumber * TimeUnit.MILLISECONDS.toNanos(periodMillis);
            var diffNanos = actualNanos - idealNanos;
            out.printf("Executing schedule %03d at %s, which is %s too late%n", scheduleNumber, formatNanos(actualNanos), formatNanos(diffNanos));

            milliSleep(sleepMillis.get(sleepMillisIndex.getAndIncrement()));
            if (sleepMillisIndex.intValue() == sleepMillis.size()) sleepMillisIndex.setValue(0);
        }, 0, periodMillis, TimeUnit.MILLISECONDS);

        waitForEnterToContinue();
        schedule.cancel(false);
    }
}
