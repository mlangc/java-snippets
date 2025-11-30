package at.mlangc.concurrent.scheduled.executor.stress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class ScheduleUsingVirtualThreads {
    private static final Logger LOG = LogManager.getLogger(ScheduleUsingVirtualThreads.class);

    static void main() {
        var schedule = scheduleWithFixedDelayUsingVirtualThread(
                () -> LOG.info("Running scheduled task"),
                0, 1, TimeUnit.SECONDS);

        IO.readln("Press enter to block vthreads from being scheduled\n");

        AtomicBoolean stopBurningCpu = new AtomicBoolean();
        Runnable burnCpu = () -> {
            LOG.info("CPU burner started");
            var rng = ThreadLocalRandom.current();

            while (!stopBurningCpu.get()) {
                if (rng.nextLong() == 42 && rng.nextLong() == 43 && rng.nextLong() == 44) {
                    LOG.info("Lucky numbers to make sure JIT doesn't optimize this away");
                }
            }
        };

        IntStream.range(0, Runtime.getRuntime().availableProcessors() * 2)
                .forEach(_ -> Thread.ofVirtual().start(burnCpu));

        IO.readln("Press enter to unblock vthreads from being scheduled\n");
        stopBurningCpu.set(true);

        IO.readln("Press enter to exit\n");
        schedule.cancel();
    }

    interface Schedule {
        void cancel();
    }

    static Schedule scheduleWithFixedDelayUsingVirtualThread(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        var cancelled = new AtomicBoolean();
        var thread = Thread.ofVirtual().unstarted(() -> {
            try {
                Thread.sleep(Duration.of(initialDelay, unit.toChronoUnit()));

                while (!cancelled.get()) {
                    command.run();
                    Thread.sleep(Duration.of(delay, unit.toChronoUnit()));
                }
            } catch (InterruptedException e) {
                // Nothing to do, we are already terminating the thread
            }
        });

        Schedule schedule = () -> cancelled.set(true);
        thread.start();
        return schedule;
    }
}
