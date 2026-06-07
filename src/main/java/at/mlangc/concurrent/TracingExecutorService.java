package at.mlangc.concurrent;

import com.google.common.util.concurrent.ForwardingExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TracingExecutorService extends ForwardingExecutorService  {
    private static final Logger LOG = LoggerFactory.getLogger(TracingExecutorService.class);

    private final ExecutorService wrappedExecutor;

    public TracingExecutorService(ExecutorService wrappedExecutor) {
        this.wrappedExecutor = wrappedExecutor;
    }

    @Override
    protected ExecutorService delegate() {
        return wrappedExecutor;
    }

    @Override
    public void execute(Runnable command) {
        LOG.trace("execute: {}", command);
        super.execute(command);
    }

    @Override
    public Future<?> submit(Runnable task) {
        LOG.trace("submit: {}", task);
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        LOG.trace("submit: {}", task);
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        LOG.trace("submit: task={}, result={}", task, result);
        return super.submit(task, result);
    }
}
