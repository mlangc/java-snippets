package at.mlangc.concurrent.scheduled.executor.stress;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class ExecutorStressHelpers {
    static String formatNanos(long nanos) {
        return formatNanos(nanos, 12);
    }

    static String formatNanos(long nanos, int totalDigits) {
        var work = new char[1 + totalDigits + totalDigits / 3 + 2];
        Arrays.fill(work, ' ');
        work[work.length - 2] = 'n';
        work[work.length - 1] = 's';

        var negatative = false;
        if (nanos < 0) {
            negatative = true;
            nanos = -nanos;
        }

        var digits = 0;
        var i = work.length - 3;
        while (nanos > 0 && digits < totalDigits) {
            if (digits != 0 && digits % 3 == 0) {
                work[i--] = ',';
            }

            var d = nanos % 10;
            nanos /= 10;
            work[i--] = (char) ('0' + d);
            digits++;
        }

        if (negatative) {
            work[i] = '-';
        }

        return new String(work);
    }

    static void waitForEnterToContinue() {
        out.println("Please press enter to continue:");
        new Scanner(System.in).nextLine();
    }

    static CompletableFuture<List<Integer>> burnCpu(int parallelism, AtomicBoolean stop) {
        var futures = IntStream.range(0, parallelism)
                .mapToObj(_ -> CompletableFuture.supplyAsync(() -> {
                    var f0 = 1;
                    var f1 = 1;

                    while (!stop.get()) {
                        var f2 = f0 + f1;
                        f0 = f1;
                        f1 = f2;

                        if (f1 >= 1_000_000_007) {
                            f1 -= 1_000_000_007;
                        }
                    }

                    return f1;
                })).toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> futures.stream().map(CompletableFuture::join).toList());
    }

    static void milliSleep(long millis) {
        if (millis <= 0) return;

        try {
            Thread.sleep(Duration.ofMillis(millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    interface Ticker {
        long readNanos();

        default long readMillis() {
            return read(TimeUnit.MILLISECONDS);
        }

        default long read(TimeUnit timeUnit) {
            return timeUnit.convert(readNanos(), TimeUnit.NANOSECONDS);
        }

        default Ticker resetZeroToNow() {
            var now = readNanos();
            return () -> readNanos() - now;
        }

        Ticker SYSTEM = System::nanoTime;
    }
}
