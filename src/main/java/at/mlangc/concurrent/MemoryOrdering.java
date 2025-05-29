package at.mlangc.concurrent;

import java.util.concurrent.atomic.*;

public enum MemoryOrdering {
    VOLATILE, ACQUIRE_RELEASE, OPAQUE, PLAIN;

    public int get(AtomicInteger atomic) {
        return switch (this) {
            case PLAIN -> atomic.getPlain();
            case OPAQUE -> atomic.getOpaque();
            case ACQUIRE_RELEASE -> atomic.getAcquire();
            case VOLATILE -> atomic.get();
        };
    }

    public void set(AtomicInteger atomic, int value) {
        switch (this) {
            case PLAIN -> atomic.setPlain(value);
            case OPAQUE -> atomic.getOpaque();
            case ACQUIRE_RELEASE -> atomic.setRelease(value);
            case VOLATILE -> atomic.set(value);
        }
    }

    public int get(AtomicIntegerArray atomicArray, int index) {
        return switch (this) {
            case PLAIN -> atomicArray.getPlain(index);
            case OPAQUE -> atomicArray.getOpaque(index);
            case ACQUIRE_RELEASE -> atomicArray.getAcquire(index);
            case VOLATILE -> atomicArray.get(index);
        };
    }

    public void set(AtomicIntegerArray atomicArray, int index, int value) {
        switch (this) {
            case PLAIN -> atomicArray.setPlain(index, value);
            case OPAQUE -> atomicArray.setOpaque(index, value);
            case ACQUIRE_RELEASE -> atomicArray.setRelease(index, value);
            case VOLATILE -> atomicArray.set(index, value);
        }
    }

    public long get(AtomicLongArray atomicArray, int index) {
        return switch (this) {
            case PLAIN -> atomicArray.getPlain(index);
            case OPAQUE -> atomicArray.getOpaque(index);
            case ACQUIRE_RELEASE -> atomicArray.getAcquire(index);
            case VOLATILE -> atomicArray.get(index);
        };
    }

    public void set(AtomicLongArray atomicArray, int index, long value) {
        switch (this) {
            case PLAIN -> atomicArray.setPlain(index, value);
            case OPAQUE -> atomicArray.setOpaque(index, value);
            case ACQUIRE_RELEASE -> atomicArray.setRelease(index, value);
            case VOLATILE -> atomicArray.set(index, value);
        }
    }

   public  <T> T get(AtomicReferenceArray<T> atomicArray, int index) {
        return switch (this) {
            case PLAIN -> atomicArray.getPlain(index);
            case OPAQUE -> atomicArray.getOpaque(index);
            case ACQUIRE_RELEASE -> atomicArray.getAcquire(index);
            case VOLATILE -> atomicArray.get(index);
        };
    }

    public <T> void set(AtomicReferenceArray<T> atomicArray, int index, T value) {
        switch (this) {
            case PLAIN -> atomicArray.setPlain(index, value);
            case OPAQUE -> atomicArray.setOpaque(index, value);
            case ACQUIRE_RELEASE -> atomicArray.setRelease(index, value);
            case VOLATILE -> atomicArray.set(index, value);
        }
    }

    public boolean get(AtomicBoolean atomic) {
        return switch (this) {
            case PLAIN -> atomic.getPlain();
            case OPAQUE -> atomic.getOpaque();
            case ACQUIRE_RELEASE -> atomic.getAcquire();
            case VOLATILE -> atomic.get();
        };
    }

    public void set(AtomicBoolean atomic, boolean value) {
        switch (this) {
            case PLAIN -> atomic.setPlain(value);
            case OPAQUE -> atomic.getOpaque();
            case ACQUIRE_RELEASE -> atomic.setRelease(value);
            case VOLATILE -> atomic.set(value);
        }
    }

    public boolean weakCompareAndSet(AtomicInteger atomic, int expectedValue, int newValue) {
        return switch (this) {
            case PLAIN -> atomic.weakCompareAndSetPlain(expectedValue, newValue);
            case OPAQUE -> throw new UnsupportedOperationException();
            case ACQUIRE_RELEASE -> atomic.weakCompareAndSetAcquire(expectedValue, newValue);
            case VOLATILE -> atomic.weakCompareAndSetVolatile(expectedValue, newValue);
        };
    }
}
