package at.mlangc.concurrent.get.opaque;

import java.util.concurrent.atomic.AtomicBoolean;

public class BrokenButAccidentallyWorkingLoopDisassembly {
final AtomicBoolean stop = new AtomicBoolean();

void run() {
    if (stop.getPlain()) {
        goBackToInterpreter();
    }

    var spins = 0;
    do {
        spins++;
        pollForSafePoint();
    } while (spins % 1_000_000_000 != 0);

    goBackToInterpreter();
}

    void pollForSafePoint() {

    }

    void goBackToInterpreter() {

    }

    void runRaw() {
        if (stop.getPlain()) { // 0x0000022d9df442e3
            goBackToInterpreter(); // 0x0000022d9df442e6
        }

        int r9;
        int r10;
        int rbx = 0;
        int r11 = 0;
        do {
            rbx++; // 0x0000022d9df442f4
            r9 = rbx; // 0x0000022d9df442f8
            r9 &= 0x3fffffff; // 0x0000022d9df442fb
            r10 = -1; // 0x0000022d9df44305
            if (r11 >= r9) {
                r10 = 0; // 0x0000022d9df4430d
            }

            pollForSafePoint(); // 0x0000022d9df44315
        } while (r9 != 0);

        goBackToInterpreter();
    }

}
