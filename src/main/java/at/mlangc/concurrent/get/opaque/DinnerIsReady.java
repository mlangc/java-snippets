package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.atomic.AtomicBoolean;

public class DinnerIsReady {
    int dinner = -1;
    final AtomicBoolean dinnerIsReady = new AtomicBoolean();

    void thread1() {
        dinner = prepareDinner();
        dinnerIsReady.setOpaque(true);
    }

    void thread2() {
        if (dinnerIsReady.getOpaque()) {
            // Might fail, since no happens before relationship
            // is established by opaque writes and reads.
            assert dinner != -1;
        }
    }

    private int prepareDinner() {
        return 42;
    }
}
