package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

enum MemoryOrdering {
    VOLATILE {
        @Override
        public int get(AtomicInteger atomic) {
            return atomic.get();
        }

        @Override
        public void set(AtomicInteger atomic, int value) {
            atomic.set(value);
        }

        @Override
        public int get(AtomicIntegerArray atomicArray, int index) {
            return atomicArray.get(index);
        }

        @Override
        public void set(AtomicIntegerArray atomicArray, int index, int value) {
            atomicArray.set(index, value);
        }
    }, ACK_REL {
        @Override
        public int get(AtomicInteger atomic) {
            return atomic.getAcquire();
        }

        @Override
        public void set(AtomicInteger atomic, int value) {
            atomic.setRelease(value);
        }

        @Override
        public int get(AtomicIntegerArray atomicArray, int index) {
            return atomicArray.getAcquire(index);
        }

        @Override
        public void set(AtomicIntegerArray atomicArray, int index, int value) {
            atomicArray.setRelease(index, value);
        }
    }, OPAQUE {
        @Override
        int get(AtomicInteger atomic) {
            return atomic.getOpaque();
        }

        @Override
        void set(AtomicInteger atomic, int value) {
            atomic.setOpaque(value);
        }

        @Override
        int get(AtomicIntegerArray atomicArray, int index) {
            return atomicArray.getOpaque(index);
        }

        @Override
        void set(AtomicIntegerArray atomicArray, int index, int value) {
            atomicArray.setOpaque(index, value);
        }
    }, PLAIN {
        @Override
        int get(AtomicInteger atomic) {
            return atomic.getPlain();
        }

        @Override
        void set(AtomicInteger atomic, int value) {
            atomic.setPlain(value);
        }

        @Override
        int get(AtomicIntegerArray atomicArray, int index) {
            return atomicArray.getPlain(index);
        }

        @Override
        void set(AtomicIntegerArray atomicArray, int index, int value) {
            atomicArray.setPlain(index, value);
        }
    };

    abstract int get(AtomicInteger atomic);
    abstract void set(AtomicInteger atomic, int value);
    abstract int get(AtomicIntegerArray atomicArray, int index);
    abstract void set(AtomicIntegerArray atomicArray, int index, int value);
}
