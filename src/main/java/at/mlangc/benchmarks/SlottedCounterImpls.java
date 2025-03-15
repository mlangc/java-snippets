package at.mlangc.benchmarks;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SlottedCounterImpls {
    interface SlottedCounter {
        void increment();

        long[] counts();

        default long sum() {
            return Arrays.stream(counts()).sum();
        }
    }

    static class SlottedCounterUsingRentrantLock implements SlottedCounter {
        private final long[] counts;
        private final boolean[] slots;
        private final Lock lock = new ReentrantLock();

        SlottedCounterUsingRentrantLock(int numSlots) {
            this.counts = new long[numSlots];
            this.slots = new boolean[numSlots];
        }

        @Override
        public void increment() {
            int i;
            while (true) {
                lock.lock();
                try {
                    for (i = 0; i < slots.length; i++) {
                        if (!slots[i]) {
                            slots[i] = true;
                            break;
                        }
                    }
                } finally {
                    lock.unlock();
                }

                if (i == slots.length) {
                    Thread.onSpinWait();
                } else {
                    break;
                }
            }

            counts[i]++;

            lock.lock();
            slots[i] = false;
            lock.unlock();
        }

        @Override
        public long[] counts() {
            return counts;
        }
    }

    static class SlottedCounterUsingAtomicGuard implements SlottedCounter {
        private final long[] counts;
        private final boolean[] slots;
        private final AtomicBoolean guard = new AtomicBoolean();

        SlottedCounterUsingAtomicGuard(int numSlots) {
            this.counts = new long[numSlots];
            this.slots = new boolean[numSlots];
        }

        @Override
        public void increment() {
            int i;
            while (true) {
                i = -1;
                if (guard.weakCompareAndSetAcquire(false, true)) {
                    try {
                        for (i = 0; i < slots.length; i++) {
                            if (!slots[i]) {
                                slots[i] = true;
                                break;
                            }
                        }
                    } finally {
                        guard.setRelease(false);
                    }
                }

                if (i < 0 || i == slots.length) {
                    Thread.onSpinWait();
                } else {
                    break;
                }
            }

            counts[i]++;

            while (true) {
                if (guard.weakCompareAndSetAcquire(false, true)) {
                    slots[i] = false;
                    guard.setRelease(false);
                    break;
                } else {
                    Thread.onSpinWait();
                }
            }
        }

        @Override
        public long[] counts() {
            return counts;
        }
    }

    static class SlottedCounterUsingAtomicArray implements SlottedCounter {
        private enum State {
            USED
        }

        private final long[] counts;
        private final AtomicReferenceArray<State> states;
        private final int width;

        SlottedCounterUsingAtomicArray(int numSlots, int width) {
            this.states = new AtomicReferenceArray<>(numSlots * width);
            this.counts = new long[numSlots * width];
            this.width = width;
        }

        @Override
        public long[] counts() {
            return counts;
        }

        @Override
        public void increment() {
            int slot;
            while (true) {
                for (slot = 0; slot < states.length(); slot += width) {
                    if (states.weakCompareAndSetAcquire(slot, null, State.USED)) {
                        break;
                    }
                }

                if (slot == states.length()) {
                    Thread.onSpinWait();
                } else {
                    break;
                }
            }

            counts[slot]++;
            states.setRelease(slot, null);
        }
    }
}
