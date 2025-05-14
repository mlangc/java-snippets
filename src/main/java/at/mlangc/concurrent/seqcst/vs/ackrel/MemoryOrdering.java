package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

enum MemoryOrdering {
    SEQ_CST {
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
    };

    abstract int get(AtomicInteger atomic);
    abstract void set(AtomicInteger atomic, int value);
    abstract int get(AtomicIntegerArray atomicArray, int index);
    abstract void set(AtomicIntegerArray atomicArray, int index, int value);
}
