package at.mlangc.vmlens;

import com.vmlens.api.AllInterleavings;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

class DinnerIsReadyVmLensTest {
    final AtomicBoolean ready = new AtomicBoolean();
    int dinner;

    // Expected to succeed - but runs forever.
    @Test
    void shouldNotDetectProblemIfUpdateIsProperlyReleased() throws InterruptedException {
        try (var allInterleavings = new AllInterleavings("ok")) {
            while (allInterleavings.hasNext()) {
                ready.set(false);
                dinner = 0;

                var thread1 = new Thread(() -> {
                    dinner = 42;
                    ready.set(true);
                });


                thread1.start();
                if (ready.get()) {
                    dinner++;
                }
                thread1.join();
            }
        }
    }

    // Expected to fail - but runs forever.
    @Test
    void shouldDetectProblemIfUpdateIsReleasedTooEarly() throws InterruptedException {
        try (var allInterleavings = new AllInterleavings("nok")) {
            while (allInterleavings.hasNext()) {
                ready.set(false);
                dinner = 0;

                var thread1 = new Thread(() -> {
                    ready.set(true);
                    dinner = 42;
                });

                thread1.start();
                if (ready.get()) {
                    dinner++;
                }
                thread1.join();
            }
        }
    }
}
