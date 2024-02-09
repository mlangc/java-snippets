package at.mlangc.benchmarks;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

public class GcdImpls {
    static long euclidRecursive(long a, long b) {
        checkArgument(a >= 0 && b >= 0);
        return euclidRecursivePositive(a, b);
    }

    static long euclidRecursivePositive(long a, long b) {
        return b == 0 ? a : euclidRecursivePositive(b, a % b);
    }

    static long euclidIterative(long a, long b) {
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

    static long gcdSteinPositive1(long a, long b) {
        if (a == 0 || a == b) {
            return b;
        } else if (b == 0) {
            return a;
        } else if (a % 2 == 0 && b % 2 == 0) {
            return 2 * gcdSteinPositive1(a / 2, b / 2);
        } else if (a % 2 == 0) {
            return gcdSteinPositive1(a / 2, b);
        } else if (b % 2 == 0) {
            return gcdSteinPositive1(a, b / 2);
        } else {
            // both a and b are odd
            if (a < b) {
                return gcdSteinPositive1(a, b - a);
            } else {
                return gcdSteinPositive1(a - b, b);
            }
        }
    }

    static long gcdSteinPositive2(long a, long b) {
        if (a == 0 || a == b) {
            return b;
        } else if (b == 0) {
            return a;
        } else {
            var zerosA = Long.numberOfTrailingZeros(a);
            var zerosB = Long.numberOfTrailingZeros(b);
            var zerosAb = Math.min(zerosA, zerosB);

            if (zerosAb > 0) {
                return (1L << zerosAb) * gcdSteinPositive2(a >> zerosAb, b >> zerosAb);
            } else if (zerosA > 0) {
                return gcdSteinPositive2(a >> zerosA, b);
            } else if (zerosB > 0) {
                return gcdSteinPositive2(a, b >> zerosB);
            } else {
                // both a and b are odd
                if (a < b) {
                    return gcdSteinPositive2(a, b - a);
                } else {
                    return gcdSteinPositive2(a - b, b);
                }
            }
        }
    }

    static long gcdSteinPositive3(long a, long b) {
        if (a == 0 || a == b) {
            return b;
        } else if (b == 0) {
            return a;
        } else {
            var zerosA = Long.numberOfTrailingZeros(a);
            var zerosB = Long.numberOfTrailingZeros(b);
            var zerosAb = Math.min(zerosA, zerosB);

            if (zerosAb > 0) {
                return (1L << zerosAb) * gcdSteinPositive3(a >> zerosAb, b >> zerosAb);
            } else if (zerosA > 0) {
                return gcdSteinPositive3(a >> zerosA, b);
            } else if (zerosB > 0) {
                return gcdSteinPositive3(a, b >> zerosB);
            } else {
                // both a and b are odd
                if (a < b) {
                    var k = Long.numberOfTrailingZeros(b - a);
                    return gcdSteinPositive3(a, (b - a) >> k);
                } else {
                    var k = Long.numberOfTrailingZeros(a - b);
                    return gcdSteinPositive3((a - b) >> k, b);
                }
            }
        }
    }

    private static final String GCD_LONG_MIN_ERROR = "GCD would result in -Long.MIN_VALUE which cannot be negated";
static long gcdStein(long a, long b) {
    if (a == Long.MIN_VALUE) {
        if (b == 0 || b == Long.MIN_VALUE) {
            throw new ArithmeticException(GCD_LONG_MIN_ERROR);
        }

        return gcdSteinPositive4(abs(Long.MIN_VALUE + abs(b)), abs(b));
    } else if (b == Long.MIN_VALUE) {
        if (a == 0) {
            throw new ArithmeticException(GCD_LONG_MIN_ERROR);
        }

        return gcdSteinPositive4(abs(a), abs(abs(a) + Long.MIN_VALUE));
    } else {
        return gcdSteinPositive4(abs(a), abs(b));
    }
}

    static long gcdSteinPositive4(long a, long b) {
        if (a == 0 || a == b) {
            return b;
        } else if (b == 0) {
            return a;
        }

        var zerosA = Long.numberOfTrailingZeros(a);
        var zerosB = Long.numberOfTrailingZeros(b);
        a >>= zerosA;
        b >>= zerosB;

        while (true) {
            if (a > b) {
                var tmp = a;
                a = b;
                b = tmp;
            }

            var d = b - a;
            if (d == 0) {
                return a << Math.min(zerosA, zerosB);
            }

            d >>= Long.numberOfTrailingZeros(d);
            b = d;
        }

    }

    static long gcdSteinPositive5(long a, long b) {
        if (a == 0 || a == b) {
            return b;
        } else if (b == 0) {
            return a;
        }

        var zerosA = Long.numberOfTrailingZeros(a);
        var zerosB = Long.numberOfTrailingZeros(b);
        a >>= zerosA;
        b >>= zerosB;

        while (a != b) {
            if (a > b) {
                var tmp = a;
                a = b;
                b = tmp;
            }

            var d = b - a;
            d >>= Long.numberOfTrailingZeros(d);
            b = d;
        }

        return a << Math.min(zerosA, zerosB);
    }

    public static long gcdApacheTweaked(final long p, final long q) {
        long u = p;
        long v = q;
        if (u == 0 || v == 0) {
            if (u == Long.MIN_VALUE || v == Long.MIN_VALUE) {
                throw new ArithmeticException(GCD_LONG_MIN_ERROR);
            }
            return Math.abs(u) + Math.abs(v);
        }
        // keep u and v negative, as negative integers range down to
        // -2^63, while positive numbers can only be as large as 2^63-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        /* assert u!=0 && v!=0; */
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
        // B1. [Find power of 2]

        int zu = Long.numberOfTrailingZeros(u);
        int zv = Long.numberOfTrailingZeros(v);
        int k = Math.min(zu, zv);
        u >>= k;
        v >>= k;

        if (k == 63) {
            throw new ArithmeticException(GCD_LONG_MIN_ERROR);
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        long t = ((u & 1) == 1) ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            t >>= Long.numberOfTrailingZeros(t);
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = v - u;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1L << k); // gcd is u*2^k
    }
}
