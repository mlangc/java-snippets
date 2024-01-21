package at.mlangc.benchmarks;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.apache.commons.math3.util.ArithmeticUtils;

import static at.mlangc.benchmarks.GcdImpls.euclidIterative;
import static at.mlangc.benchmarks.GcdImpls.euclidRecursive;
import static org.assertj.core.api.Assertions.*;

public class TestGcdImpls {
    @Property
    void gcdImplementationsAreConsistent(@ForAll long a, @ForAll long b) {
        if ((a == Long.MIN_VALUE && (b == Long.MIN_VALUE || b == 0)) || (b == Long.MIN_VALUE && a == 0)) {
            assertThatRuntimeException().isThrownBy(() -> euclidIterative(a, b));
            assertThatRuntimeException().isThrownBy(() -> euclidRecursive(a, b));
            assertThatRuntimeException().isThrownBy(() -> ArithmeticUtils.gcd(a, b));
        } else {
            var gcdEuclidIterative = euclidIterative(a, b);
            var gcdEuclidRecursive = euclidRecursive(a, b);
            var gcdStein = ArithmeticUtils.gcd(a, b);

            assertThat(gcdEuclidIterative)
                    .as("a=%s, b=%s", a, b)
                    .isEqualTo(gcdEuclidRecursive)
                    .isEqualTo(gcdStein);
        }
    }
}
