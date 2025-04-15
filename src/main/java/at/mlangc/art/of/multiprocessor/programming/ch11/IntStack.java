package at.mlangc.art.of.multiprocessor.programming.ch11;

import java.util.NoSuchElementException;

public interface IntStack {
    void push(int x);
    int pop();

    default Integer tryPop() {
        try {
            return pop();
        } catch (NoSuchElementException ignore) {
            return null;
        }
    }
}
