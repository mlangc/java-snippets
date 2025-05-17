package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

enum MemoryOrdering {
    VOLATILE, ACK_REL, PLAIN;

    int get(AtomicInteger atomic) {
        return switch (this) {
            case PLAIN -> atomic.getPlain();
            case ACK_REL -> atomic.getAcquire();
            case VOLATILE -> atomic.get();
        };
    }

    void set(AtomicInteger atomic, int value) {
        switch (this) {
            case PLAIN -> atomic.setPlain(value);
            case ACK_REL -> atomic.setRelease(value);
            case VOLATILE -> atomic.set(value);
        }
    }

    int get(AtomicIntegerArray atomicArray, int index) {
        return switch (this) {
            case PLAIN -> atomicArray.getPlain(index);
            case ACK_REL -> atomicArray.getAcquire(index);
            case VOLATILE -> atomicArray.get(index);
        };
    }

    void set(AtomicIntegerArray atomicArray, int index, int value) {
        switch (this) {
            case PLAIN -> atomicArray.setPlain(index, value);
            case ACK_REL -> atomicArray.setRelease(index, value);
            case VOLATILE -> atomicArray.set(index, value);
        }
    }
}
