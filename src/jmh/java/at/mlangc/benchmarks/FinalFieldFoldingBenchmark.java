package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class FinalFieldFoldingBenchmark {
    public enum Expression {
        DIV {
            @Override
            double eval(double x, double y) {
                return x / y;
            }
        },
        POW {
            @Override
            double eval(double x, double y) {
                return Math.pow(x, y);
            }
        };

        abstract double eval(double x, double y);
    }

    public record Xy(double x, double y, Expression expr) {
        double z() {
            return expr.eval(x, y);
        }
    }

    @Param("3.1415926")
    private double x;

    @Param("2.718282")
    private double y;

    @Param("DIV")
    private Expression expr;

    private double result;

    private Xy xy;

    @Setup
    public void setup() {
        result = expr.eval(x, y);
        xy = new Xy(x, y, expr);
    }

    @Benchmark
    public double returnResult() {
        return result;
    }

    @Benchmark
    public double calcDirectly() {
        return expr.eval(x, y);
    }

    @Benchmark
    public double calcViaXy() {
        return xy.z();
    }
}
