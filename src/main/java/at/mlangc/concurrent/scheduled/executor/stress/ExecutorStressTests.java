package at.mlangc.concurrent.scheduled.executor.stress;

import com.google.common.util.concurrent.UncheckedTimeoutException;

import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;

public class ExecutorStressTests {
    static void main() throws Exception {
        Locale.setDefault(Locale.ENGLISH);

        scheduleDelayedByCpuStarvation(1, -1);
    }

    interface Ticker {
        long readNanos();

        default long readMillis() {
            return read(TimeUnit.MILLISECONDS);
        }

        default long read(TimeUnit timeUnit) {
            return timeUnit.convert(readNanos(), TimeUnit.NANOSECONDS);
        }

        default Ticker resetZeroToNow() {
            var now = readNanos();
            return () -> readNanos() - now;
        }

        Ticker SYSTEM = System::nanoTime;
    }

    static abstract class StressTest {
        final Ticker ticker = getOrCreateTicker();

        Ticker getOrCreateTicker() {
            return Ticker.SYSTEM;
        }

        ScheduledExecutorService getOrCreateExecutor() {
            return Executors.newSingleThreadScheduledExecutor();
        }

        abstract void run(ScheduledExecutorService executor) throws Exception;

        final void run() throws Exception {
            var executor = getOrCreateExecutor();
            try {
                run(executor);
            } finally {
                shutdownExecutor(executor);
            }
        }

        void shutdownExecutor(ScheduledExecutorService executor) throws Exception {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                throw new UncheckedTimeoutException("Shutdown timed out");
            }
        }
    }

    static class WillScheduleAtFixedRateReadjust extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws Exception {
            var rateMillis = 100;
            var ticker = this.ticker.resetZeroToNow();
            var nextInvocationNanos = new AtomicLong(ticker.readNanos());
            var schedule = executor.scheduleAtFixedRate(() -> {
                var actualInvocationNanos = ticker.readNanos();
                var offsetNanos = actualInvocationNanos - nextInvocationNanos.get();
                nextInvocationNanos.updateAndGet(x -> x + TimeUnit.MILLISECONDS.toNanos(rateMillis));
                out.printf("Schedule invoked at %,015dns with an offset of %,012dns%n", actualInvocationNanos, offsetNanos);
            }, 0, rateMillis, TimeUnit.MILLISECONDS);

            ExecutorStressHelpers.milliSleep(500);

            executor.execute(() -> {
                var t0 = ticker.readNanos();
                ExecutorStressHelpers.milliSleep(2000);
                var t1 = ticker.readNanos();
                out.printf("Delayer slept from %,015dns to %,015dns%n", t0, t1);
            });

            ExecutorStressHelpers.waitForEnterToContinue();
            schedule.cancel(false);
        }


    }

    private static class ScheduleDelayedByExecute extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws ExecutionException, InterruptedException {
            var t0 = ticker.readMillis();
            var schedule = executor.schedule(() -> out.printf("running @ t=%sms%n", ticker.readMillis() - t0), 100, TimeUnit.MILLISECONDS);

            for (int i = 0; i < 1000; i++) {
                executor.execute(() -> ExecutorStressHelpers.milliSleep(1));
            }

            schedule.get();
        }
    }

    private static class ScheduleAtFixedRateNotCatchingUp extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws Exception {
            var t0 = System.currentTimeMillis();
            var last = new AtomicLong(t0);
            var schedule = executor.scheduleAtFixedRate(() -> {
                var t = ticker.readMillis();
                out.printf("running @ t=%sms, d=%s%n", t - t0, t - last.longValue());
                last.set(t);
                ExecutorStressHelpers.milliSleep(250);
            }, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(2000);
            schedule.cancel(false);
        }
    }


    private static void scheduleDelayedByCpuStarvation(int parallelism, int priority) throws InterruptedException {
        out.printf("Running sleep test while burning CPU on %s threads with priority %s:%n%n", parallelism, priority);

        var stop = new AtomicBoolean();
        var future = ExecutorStressHelpers.burnCpu(parallelism, stop);

        (priority < Thread.MIN_PRIORITY ? Thread.ofVirtual() : Thread.ofPlatform().priority(priority))
                .name("test")
                .start(() -> {
                    while (!stop.get()) {
                        var sleepMillis = 500;
                        var t0 = System.nanoTime();
                        ExecutorStressHelpers.milliSleep(sleepMillis);
                        var elapsed = System.nanoTime() - t0;
                        out.printf("slept for %s, which is %s too long%n", ExecutorStressHelpers.formatNanos(elapsed), ExecutorStressHelpers.formatNanos(elapsed - TimeUnit.MILLISECONDS.toNanos(sleepMillis)));
                    }
                });

        ExecutorStressHelpers.waitForEnterToContinue();
        stop.set(true);
        future.join();
    }
}
