package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * See <a href="https://www.reddit.com/r/java/comments/1m90btd/comment/n5bsniu/?utm_source=share&utm_medium=web3x&utm_name=web3xcss&utm_term=1&utm_content=share_button">Method Handles faster reflection (sometimes)</a>
 */
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class MethodHandleBenchmark {
    private static final MethodHandle METHOD_HANDLE;
    private static final Method METHOD;

    static {
        try {
            METHOD_HANDLE = MethodHandles.lookup().findStatic(Integer.class, "compare", MethodType.methodType(int.class, int.class, int.class));
            METHOD = Integer.class.getMethod("compare", int.class, int.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Benchmark
    public int baseline() {
        return -1;
    }

    @Benchmark
    public int directInvoke() {
        return Integer.compare(1, 2);
    }

    @Benchmark
    public int methodHandleInvokeExact() throws Throwable {
        return (int) METHOD_HANDLE.invokeExact(1, 2);
    }

    @Benchmark
    public int methodHandleInvoke() throws Throwable {
        return (int) METHOD_HANDLE.invoke(1, 2);
    }

    @Benchmark
    public int reflectiveInvoke() throws InvocationTargetException, IllegalAccessException {
        return (int) METHOD.invoke(null, 1, 2);
    }

    @Benchmark
    public int methodHandleInvokeWithArgs() throws Throwable {
        return (int) METHOD_HANDLE.invokeWithArguments(1, 2);
    }
}