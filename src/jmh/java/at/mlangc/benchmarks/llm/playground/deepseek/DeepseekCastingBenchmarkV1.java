package at.mlangc.benchmarks.llm.playground.deepseek;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class DeepseekCastingBenchmarkV1 {

    private Object stringObject;
    private Object integerObject;
    private String stringValue;
    private Integer integerValue;

    @Setup
    public void setup() {
        stringValue = "Hello, World!";
        integerValue = 42;
        stringObject = stringValue;
        integerObject = integerValue;
    }

    // Baseline - no casting
    @Benchmark
    public String baselineStringNoCast() {
        return stringValue;
    }

    @Benchmark
    public Integer baselineIntegerNoCast() {
        return integerValue;
    }

    // Direct cast from exact type
    @Benchmark
    public String directCastFromString() {
        return (String) stringValue;
    }

    @Benchmark
    public Integer directCastFromInteger() {
        return (Integer) integerValue;
    }

    // Cast from Object that always succeeds
    @Benchmark
    public String castStringFromObject() {
        return (String) stringObject;
    }

    @Benchmark
    public Integer castIntegerFromObject() {
        return (Integer) integerObject;
    }

    // Method that requires casting
    @Benchmark
    public void methodWithCasting(Blackhole bh) {
        bh.consume((String) stringObject);
        bh.consume((Integer) integerObject);
    }

    // Method without casting
    @Benchmark
    public void methodWithoutCasting(Blackhole bh) {
        bh.consume(stringValue);
        bh.consume(integerValue);
    }

    // Multiple consecutive casts
    @Benchmark
    public void multipleCasts(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            bh.consume((String) stringObject);
        }
    }

    // Interface casting
    private interface TestInterface {
        void doSomething();
    }

    private static class TestClass implements TestInterface {
        @Override
        public void doSomething() {
            // No-op
        }
    }

    private TestInterface interfaceRef = new TestClass();
    private Object interfaceObject = new TestClass();

    @Benchmark
    public void interfaceCast(Blackhole bh) {
        TestClass casted = (TestClass) interfaceRef;
        bh.consume(casted);
    }

    @Benchmark
    public void interfaceCastFromObject(Blackhole bh) {
        TestClass casted = (TestClass) interfaceObject;
        bh.consume(casted);
    }
}