package at.mlangc.art.of.multiprocessor.programming.ch11;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JavaUtilConcurrentIntExchanger implements IntExchanger {
    private final Exchanger<Integer> exchanger = new Exchanger<>();

    @Override
    public Response exchange(Integer x, long timeout, TimeUnit unit) {
        try {
            return new Exchanged(exchanger.exchange(x, timeout, unit));
        } catch (TimeoutException e) {
            return Response.TIMED_OUT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }
}
