package at.mlangc.vmlens;

import com.vmlens.api.AllInterleavings;
import org.junit.jupiter.api.Test;

public class TestNonVolatileField {
    private int j = 0;
    @Test
    public void testUpdate() throws InterruptedException {
        try(AllInterleavings allInterleavings = new AllInterleavings("testNonVolatileField")) {
            while (allInterleavings.hasNext()) {
                Thread first = new Thread(() -> j++);
                first.start();
                j++;
                first.join();
            }
        }
    }
}