package at.mlangc.art.of.multiprocessor.programming.ch10;

import java.util.NoSuchElementException;

public interface IntQueue {
    void enq(int x);
    int deq();

    default Integer tryDeq() {
        try {
            return deq();
        } catch (NoSuchElementException ignore) {
            return null;
        }
    }
}
