package at.mlangc.art.of.multiprocessor.programming.ch11;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeExchanger implements Exchanger {
    private record State(Thread owner1, Thread owner2, Integer value)  {

    }

    private final AtomicReference<State> state = new AtomicReference<>();

    @Override
    public Response exchange(Integer x, long timeout, TimeUnit unit) {
        final var deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        final var maxSpinsBeforeNanoTime = 10;

        var spinsSinceNanoTime = 0;
        while (true) {
            State currentState = state.getAcquire();
            if (currentState == null) {
                var newState = new State(Thread.currentThread(), null, x);
                state.weakCompareAndSetRelease(null, newState);
            } else if (currentState.owner2 == null && currentState.owner1 != Thread.currentThread()) {
                var newState = new State(currentState.owner1, Thread.currentThread(), x);
                if (state.weakCompareAndSetRelease(currentState, newState)) {
                    return new Exchanged(currentState.value);
                }
            } else if (currentState.owner2 != null && currentState.owner1 == Thread.currentThread()) {
                state.setOpaque(null);
                return new Exchanged(currentState.value);
            } else {
                Thread.onSpinWait();

                if (++spinsSinceNanoTime >= maxSpinsBeforeNanoTime) {
                    if (System.nanoTime() > deadlineNanos) {
                        if (state.weakCompareAndSetPlain(currentState, null)) {
                            return Response.TIMED_OUT;
                        }
                    }

                    spinsSinceNanoTime = 0;
                }
            }
        }
    }
}
