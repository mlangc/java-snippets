package at.mlangc.concurrent.task.dispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SynchronizingTaskDispatcherDeadlockDemo {
    static void main() {
        scenarioWithNonBlockingReentrantCallHittingInFlightLimit();
    }

    static void scenarioWithNonBlockingReentrantCall() {
        var dispatcher = new SynchronizingTaskDispatcher<String>(100);

        var future = dispatcher.dispatchAsync("x", () -> dispatcher.dispatch("x", () -> 42));
        assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(42);
    }

    static void scenarioWithNonBlockingReentrantCallHittingInFlightLimit() {
        var dispatcher = new SynchronizingTaskDispatcher<String>(1);

        assertThat(CompletableFuture.runAsync(() -> {
            var future = dispatcher.dispatchAsync("x", () -> dispatcher.dispatch("x", () -> 42));
            assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(42);
        })).succeedsWithin(2, TimeUnit.SECONDS);
    }

    static void scenarioWithBlockingReentrantCall() {
        var dispatcher = new SynchronizingTaskDispatcher<String>(100);

        var future = dispatcher.dispatch("x", () -> dispatcher.dispatch("x", () -> 42).join());
        assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(42);
    }
}
