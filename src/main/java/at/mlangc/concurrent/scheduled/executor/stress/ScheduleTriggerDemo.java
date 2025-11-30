package at.mlangc.concurrent.scheduled.executor.stress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleTriggerDemo {
    void scheduleDirectly(ScheduledExecutorService executor) {
        executor.schedule(this::longRunningAction, 1, TimeUnit.SECONDS);
    }

    void scheduleIndirectly(ScheduledExecutorService scheduler, ExecutorService worker) {
        scheduler.schedule(() -> worker.execute(this::longRunningAction), 1, TimeUnit.SECONDS);
    }

    void longRunningAction() {

    }
}
