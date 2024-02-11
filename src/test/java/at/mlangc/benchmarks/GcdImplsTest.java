package at.mlangc.benchmarks;

import com.google.common.math.LongMath;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.apache.commons.numbers.core.ArithmeticUtils;

import static at.mlangc.benchmarks.GcdImpls.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class GcdImplsTest {
    @Property
    void gcdImplementationsShouldBeConsistentOnPositiveNumbers(
            @ForAll @LongRange(min = 0) long a,
            @ForAll @LongRange(min = 0) long b) {
        var gcdEuclidIterative = euclidIterative(a, b);
        var gcdEuclidRecursive = euclidRecursive(a, b);
        var gcdApache = ArithmeticUtils.gcd(a, b);
        var gcdApacheTweaked = gcdApacheTweaked(a, b);
        var gcdGuava = LongMath.gcd(a, b);
        var gcdGuavaTweaked = gcdGuavaTweaked(a, b);
        var stein1 = gcdSteinPositive1(a, b);
        var stein2 = gcdSteinPositive2(a, b);
        var stein3 = gcdSteinPositive3(a, b);
        var stein4 = gcdSteinPositive4(a, b);
        var stein5 = gcdSteinPositive5(a, b);

        assertThat(gcdEuclidIterative)
                .as("a=%s, b=%s", a, b)
                .isEqualTo(gcdEuclidRecursive)
                .isEqualTo(gcdApache)
                .isEqualTo(gcdApacheTweaked)
                .isEqualTo(stein1)
                .isEqualTo(stein2)
                .isEqualTo(stein3)
                .isEqualTo(stein4)
                .isEqualTo(stein5)
                .isEqualTo(gcdGuavaTweaked)
                .isEqualTo(gcdGuava);
    }

    @Property
    void gcdImplementationsShouldBeConsistentOnAllNumbers(@ForAll long a, @ForAll long b) {
        try {
            var gcdApache = ArithmeticUtils.gcd(a, b);
            var gcdApacheTweaked = gcdApacheTweaked(a, b);
            var gcdStein = gcdStein(a, b);
            assertThat(gcdApache).isEqualTo(gcdApacheTweaked).isEqualTo(gcdStein);
        } catch (ArithmeticException e) {
            assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> gcdApacheTweaked(a, b));
            assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> gcdStein(a, b));
        }
    }
}
