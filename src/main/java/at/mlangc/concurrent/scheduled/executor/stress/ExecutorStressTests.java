package at.mlangc.concurrent.scheduled.executor.stress;

import at.mlangc.micrometer.MicrometerRegistryProvider;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.milliSleep;
import static at.mlangc.concurrent.scheduled.executor.stress.ExecutorStressHelpers.waitForEnterToContinue;

public class ExecutorStressTests {
    private static final Logger LOG = LogManager.getLogger(ExecutorStressTests.class);

    static void main() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        new SchedulePermanentlyDelayedByExecute().run();
    }

    static abstract class StressTest {
        final ExecutorStressHelpers.Ticker ticker = getOrCreateTicker();

        ExecutorStressHelpers.Ticker getOrCreateTicker() {
            return ExecutorStressHelpers.Ticker.SYSTEM;
        }

        ScheduledExecutorService getOrCreateExecutor() {
            return Executors.newSingleThreadScheduledExecutor();
        }

        abstract void run(ScheduledExecutorService executor) throws Exception;

        final void run() throws Exception {
            var meterRegistry = MicrometerRegistryProvider.newDynatraceMeterRegistry();
            var executor = getOrCreateExecutor();
            var metrics = new ExecutorServiceMetrics(executor, getClass().getSimpleName(), "java.snippets", List.of());
            metrics.bindTo(meterRegistry);

            try {
                run(executor);
            } finally {
                shutdownExecutor(executor);
                meterRegistry.close();
            }
        }

        void shutdownExecutor(ScheduledExecutorService executor) throws Exception {
            executor.shutdownNow();
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
                LOG.printf(Level.INFO, "Schedule invoked at %,015dns with an offset of %,012dns", actualInvocationNanos, offsetNanos);
            }, 0, rateMillis, TimeUnit.MILLISECONDS);

            ExecutorStressHelpers.milliSleep(500);

            executor.execute(() -> {
                var t0 = ticker.readNanos();
                ExecutorStressHelpers.milliSleep(2000);
                var t1 = ticker.readNanos();
                LOG.printf(Level.INFO, "Delayer slept from %,015dns to %,015dns", t0, t1);
            });

            waitForEnterToContinue();
            schedule.cancel(false);
        }


    }

    private static class ScheduleDelayedByExecute extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws ExecutionException, InterruptedException {
            var t0 = ticker.readMillis();
            var schedule = executor.schedule(() -> LOG.printf(Level.INFO, "running @ t=%sms", ticker.readMillis() - t0), 100, TimeUnit.MILLISECONDS);

            for (int i = 0; i < 1000; i++) {
                executor.execute(() -> ExecutorStressHelpers.milliSleep(1));
            }

            schedule.get();
        }
    }

    private static class SchedulePermanentlyDelayedByExecute extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws Exception {
            var periodMillis = 100;

            var taskSubmitter = Thread.ofVirtual().start(() -> {
                try {
                    LOG.info("Submitting a task which blocks for {} millis every {} millis", periodMillis, periodMillis);
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(periodMillis);

                        executor.execute(() -> {
                            LOG.info("About to sleep {} millis", periodMillis);
                            milliSleep(periodMillis);
                        });
                    }
                } catch (InterruptedException e) {
                    // OK - just exit the thread
                }
            });

            LOG.info("Submitting a schedule with a fixed rate of {} millis", periodMillis);

            var schedule = executor.scheduleAtFixedRate(
                    () -> LOG.info("Executing task scheduled at rate of {} millis", periodMillis),
                    0, periodMillis, TimeUnit.MILLISECONDS);

            waitForEnterToContinue();
            taskSubmitter.interrupt();
            schedule.cancel(false);
        }
    }

    private static class ScheduleAtFixedRateNotCatchingUp extends StressTest {
        @Override
        void run(ScheduledExecutorService executor) throws Exception {
            var t0 = System.currentTimeMillis();
            var last = new AtomicLong(t0);
            var schedule = executor.scheduleAtFixedRate(() -> {
                var t = ticker.readMillis();
                LOG.printf(Level.INFO, "running @ t=%sms, d=%s", t - t0, t - last.longValue());
                last.set(t);
                ExecutorStressHelpers.milliSleep(250);
            }, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(2000);
            schedule.cancel(false);
        }
    }


    private static void scheduleDelayedByCpuStarvation(int parallelism, int priority) throws InterruptedException {
        LOG.printf(Level.INFO, "Running sleep test while burning CPU on %s threads with priority %s:", parallelism, priority);

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
                        LOG.printf(Level.INFO, "slept for %s, which is %s too long",
                                ExecutorStressHelpers.formatNanos(elapsed), ExecutorStressHelpers.formatNanos(elapsed - TimeUnit.MILLISECONDS.toNanos(sleepMillis)));
                    }
                });

        waitForEnterToContinue();
        stop.set(true);
        future.join();
    }
}
