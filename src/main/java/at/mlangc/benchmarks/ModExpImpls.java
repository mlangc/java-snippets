package at.mlangc.benchmarks;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ModExpImpls {
    private final long[] branches = new long[2];

    public long modPowClassic(long a, long b, long mod) {
        var res = 1L;
        while (b != 0) {
            if ((b & 1) != 0) {
                res = (res * a) % mod;
            }

            a = (a * a) % mod;
            b >>>= 1;
        }

        return res;
    }

    public long modPowBranchLess(long a, long b, long mod) {
        var res = 1L;
        while (b != 0) {
            branches[0] = res;
            branches[1] = (res * a) % mod;
            res = branches[(int) (b & 1)];
            a = (a * a) % mod;
            b >>>= 1;
        }

        return res;
    }

    public long maskPowClassic(long a, long b, long mask) {
        var res = 1L;
        while (b != 0) {
            if ((b & 1) != 0) {
                res = (res * a) & mask;
            }

            a = (a * a) & mask;
            b >>>= 1;
        }

        return res;
    }

    public long maskPowBranchLess(long a, long b, long mask) {
        var res = 1L;
        while (b != 0) {
            branches[0] = res;
            branches[1] = (res * a) & mask;
            res = branches[(int) (b & 1)];
            a = (a * a) & mask;
            b >>>= 1;
        }

        return res;
    }
}
