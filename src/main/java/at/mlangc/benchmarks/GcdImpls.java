package at.mlangc.benchmarks;

import static com.google.common.base.Preconditions.checkArgument;

public class GcdImpls {
    public static long euclidRecursive(long a, long b) {
        checkArgument(a >= 0 && b >= 0);
        return euclidRecursive0(a, b);
    }

    private static long euclidRecursive0(long a, long b) {
        return b == 0 ? a : euclidRecursive0(b, a % b);
    }

    public static long euclidIterative(long a, long b) {
        checkArgument(a >= 0 && b >= 0);
        return euclidIterative0(a, b);
    }

    private static long euclidIterative0(long a, long b) {
        while (b != 0) {
            var bb = a % b;
            a = b;
            b = bb;
        }

        return a;
    }
}
