package at.mlangc.benchmarks;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModExpImplsTest {
    private final ModExpImpls impls = new ModExpImpls();

    @Test
    void modPowShouldWorkWithTrivialExamples() {
        assertThat(impls.modPowClassic(1, 0, 2)).isOne();
        assertThat(impls.modPowClassic(1, 1, 2)).isOne();
        assertThat(impls.modPowClassic(1, 100, 2)).isOne();
    }

    @Test
    void modPowShouldWorkWithSimpleExamples() {
        assertThat(impls.modPowClassic(2, 2, 8)).isEqualTo(4);
        assertThat(impls.modPowClassic(3, 3, 9)).isZero();
        assertThat(impls.modPowClassic(2, 10, 1_000_000)).isEqualTo(1024);
    }

    @Property
    void modPowImplsShouldBeConsistent(
            @ForAll @LongRange(min = 1, max = 100) long a,
            @ForAll @LongRange(min = 0) long b,
            @ForAll @LongRange(min = 2, max = Integer.MAX_VALUE) long mod) {
        a %= mod;
        var resClassic = impls.modPowClassic(a, b, mod);
        var resBranchLess = impls.modPowBranchLess(a, b, mod);
        assertThat(resClassic).as("a=%s, b=%s, mod=%s", a, b, mod).isEqualTo(resBranchLess);
    }

    @Property
    void modPowShouldBeConsistentWithMaskPowForPowersOfTwo(
            @ForAll @LongRange(min = 1, max = 100) long a,
            @ForAll @LongRange(min = 0) long b,
            @ForAll @LongRange(min = 0, max = 31) int power) {
        var mod = 1 << power;
        var mask = mod - 1;

        a %= mod;
        assertThat(impls.modPowClassic(a, b, mod))
                .as("a=%s, b=%s, power=%s", a, b, power)
                .isEqualTo(impls.maskPowClassic(a, b, mask))
                .isEqualTo(impls.maskPowBranchLess(a, b, mask));
    }
}