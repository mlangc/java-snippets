package at.mlangc.benchmarks;

import org.openjdk.jmh.annotations.*;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ExtendedRecordHashCodeBenchmark {
    @Param("10000")
    private int numKeys;

    private KeyRecord[] recordBasedKeys;
    private TypedKeyRecord[] typedRecordBasedKeys;
    private KeyClass[] classBasedKeys;
    private TypedKeyClass[] typedClassBasedKeys;
    private int i;

    private KeyRecord nextRecordBasedKey() {
        var r = recordBasedKeys[i];
        if (++i == recordBasedKeys.length) i = 0;
        return r;
    }

    private TypedKeyRecord nextTypedRecordBasedKey() {
        var r = typedRecordBasedKeys[i];
        if (++i == typedRecordBasedKeys.length) i = 0;
        return r;
    }

    private KeyClass nextClassBasedKey() {
        var r = classBasedKeys[i];
        if (++i == classBasedKeys.length) i = 0;
        return r;
    }

    private TypedKeyClass nextTypedClassBasedKey() {
        var r = typedClassBasedKeys[i];
        if (++i == typedClassBasedKeys.length) i = 0;
        return r;
    }

    @Setup
    public void setup() {
        var rng = new Random(2025);
        recordBasedKeys = new KeyRecord[numKeys];
        classBasedKeys = new KeyClass[numKeys];
        typedRecordBasedKeys = new TypedKeyRecord[numKeys];
        typedClassBasedKeys = new TypedKeyClass[numKeys];

        for (int i = 0; i < numKeys; i++) {
            var x = rng.nextInt();
            var s = String.valueOf(x);
            recordBasedKeys[i] = new KeyRecord(x, s);
            classBasedKeys[i] = new KeyClass(x, s);
            typedRecordBasedKeys[i] = new TypedKeyRecord(x, s);
            typedClassBasedKeys[i] = new TypedKeyClass(x, s);
        }
    }

    @Benchmark
    public int classHashCode() {
        return nextClassBasedKey().hashCode();
    }

    @Benchmark
    public int typedClassHashCode() {
        return nextTypedClassBasedKey().hashCode();
    }

    @Benchmark
    public int recordHashCode() {
        return nextRecordBasedKey().hashCode();
    }

    @Benchmark
    public int typedRecordHashCode() {
        return nextTypedRecordBasedKey().hashCode();
    }

    record KeyRecord(Object key1, Object key2) {}

    record TypedKeyRecord(Integer key1, String key2) { }

    private static final class KeyClass {
        private final Object key1;
        private final Object key2;

        KeyClass(Object key1, Object key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Objects.hashCode(key1);
            result = prime * result + Objects.hashCode(key2);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KeyClass other = (KeyClass) obj;
            if (key1 == null) {
                if (other.key1 != null)
                    return false;
            } else if (!key1.equals(other.key1))
                return false;
            if (key2 == null) {
                if (other.key2 != null)
                    return false;
            } else if (!key2.equals(other.key2))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "KeyClass [key1=" + key1 + ", key2=" + key2 + "]";
        }
    }

    private static final class TypedKeyClass {
        private final Integer key1;
        private final String key2;

        TypedKeyClass(Integer key1, String key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key1 == null) ? 0 : key1.hashCode());
            result = prime * result + ((key2 == null) ? 0 : key2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TypedKeyClass other = (TypedKeyClass) obj;
            if (key1 == null) {
                if (other.key1 != null)
                    return false;
            } else if (!key1.equals(other.key1))
                return false;
            if (key2 == null) {
                if (other.key2 != null)
                    return false;
            } else if (!key2.equals(other.key2))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "KeyClass [key1=" + key1 + ", key2=" + key2 + "]";
        }
    }
}
