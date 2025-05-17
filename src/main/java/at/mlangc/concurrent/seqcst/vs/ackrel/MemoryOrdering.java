package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

enum MemoryOrdering {
    VOLATILE, ACQUIRE_RELEASE, PLAIN;

    int get(AtomicInteger atomic) {
        return switch (this) {
            case PLAIN -> atomic.getPlain();
            case ACQUIRE_RELEASE -> atomic.getAcquire();
            case VOLATILE -> atomic.get();
        };
    }

    void set(AtomicInteger atomic, int value) {
        switch (this) {
            case PLAIN -> atomic.setPlain(value);
            case ACQUIRE_RELEASE -> atomic.setRelease(value);
            case VOLATILE -> atomic.set(value);
        }
    }

    int get(AtomicIntegerArray atomicArray, int index) {
        return switch (this) {
            case PLAIN -> atomicArray.getPlain(index);
            case ACQUIRE_RELEASE -> atomicArray.getAcquire(index);
            case VOLATILE -> atomicArray.get(index);
        };
    }

    void set(AtomicIntegerArray atomicArray, int index, int value) {
        switch (this) {
            case PLAIN -> atomicArray.setPlain(index, value);
            case ACQUIRE_RELEASE -> atomicArray.setRelease(index, value);
            case VOLATILE -> atomicArray.set(index, value);
        }
    }

    boolean get(AtomicBoolean atomic) {
        return switch (this) {
            case PLAIN -> atomic.getPlain();
            case ACQUIRE_RELEASE -> atomic.getAcquire();
            case VOLATILE -> atomic.get();
        };
    }

    void set(AtomicBoolean atomic, boolean value) {
        switch (this) {
            case PLAIN -> atomic.setPlain(value);
            case ACQUIRE_RELEASE -> atomic.setRelease(value);
            case VOLATILE -> atomic.set(value);
        }
    }

}
