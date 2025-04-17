package at.mlangc.art.of.multiprocessor.programming.ch11;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ThreadSafe
public class EliminationArrayExchanger implements Exchanger {
    private final ThreadLocal<RangePolicy> rangePolicy;
    private final Exchanger[] exchangers;

    public EliminationArrayExchanger(int size, Supplier<Exchanger> exchangerFactory, RangePolicy.Factory policyFactory) {
        this.exchangers = new Exchanger[size];
        Arrays.setAll(exchangers, ignore -> exchangerFactory.get());
        this.rangePolicy = ThreadLocal.withInitial(() -> policyFactory.create(size));
    }

    @Override
    public Response exchange(Integer x, long timeout, TimeUnit unit) {
        var localRangePolicy = rangePolicy.get();
        var range = localRangePolicy.getRange();
        var slot = ThreadLocalRandom.current().nextInt(range);
        var res = exchangers[slot].exchange(x, timeout, unit);

        if (res instanceof TimedOut) {
            localRangePolicy.recordTimeout();
        } else {
            localRangePolicy.recordSuccess();
        }

        return res;
    }

    @NotThreadSafe
    public interface RangePolicy {
        void recordTimeout();
        void recordSuccess();
        int getRange();

        interface Factory {
            RangePolicy create(int maxRange);
        }

        class Constant implements RangePolicy {
            private final int range;

            public Constant(int range) {
                this.range = range;
            }

            @Override
            public void recordTimeout() {

            }

            @Override
            public void recordSuccess() {

            }

            @Override
            public int getRange() {
                return range;
            }
        }

        class Adaptive implements RangePolicy {
            private final float factor;
            private final int maxRange;
            private float range = 1;

            public Adaptive(float factor, int maxRange) {
                this.factor = factor;
                this.maxRange = maxRange;
            }

            @Override
            public void recordTimeout() {
                range = Math.max(1, Math.round(range / factor));
            }

            @Override
            public void recordSuccess() {
                range = Math.min(maxRange, range * factor);
            }

            @Override
            public int getRange() {
                return Math.round(range);
            }
        }
    }
}
