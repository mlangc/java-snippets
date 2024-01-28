package at.mlangc.benchmarks;

import com.google.common.math.LongMath;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.apache.commons.math3.util.ArithmeticUtils;

import static at.mlangc.benchmarks.GcdImpls.euclidIterative;
import static at.mlangc.benchmarks.GcdImpls.euclidRecursive;
import static org.assertj.core.api.Assertions.assertThat;

public class GcdImplsTest {
    @Property
    void gcdImplementationsAreConsistent(
            @ForAll @LongRange(min = 0) long a,
            @ForAll @LongRange(min = 0) long b) {
        var gcdEuclidIterative = euclidIterative(a, b);
        var gcdEuclidRecursive = euclidRecursive(a, b);
        var gcdCommons = ArithmeticUtils.gcd(a, b);
        var gcdGuava = LongMath.gcd(a, b);

        assertThat(gcdEuclidIterative)
                .as("a=%s, b=%s", a, b)
                .isEqualTo(gcdEuclidRecursive)
                .isEqualTo(gcdCommons)
                .isEqualTo(gcdGuava);
    }
}
