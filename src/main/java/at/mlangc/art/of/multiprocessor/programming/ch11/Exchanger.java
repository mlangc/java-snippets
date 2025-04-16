package at.mlangc.art.of.multiprocessor.programming.ch11;

import java.util.concurrent.TimeUnit;

public interface Exchanger {
    sealed interface Response {
        TimedOut TIMED_OUT = TimedOut.TIMED_OUT;
    }

    enum TimedOut implements Response {
        TIMED_OUT;
    }

    record Exchanged(Integer x) implements Response { }

    Response exchange(Integer x, long timeout, TimeUnit unit);
}
