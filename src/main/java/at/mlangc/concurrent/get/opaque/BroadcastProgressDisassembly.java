package at.mlangc.concurrent.get.opaque;

public class BroadcastProgressDisassembly {
    int progress;
    int pollPageAddr;

    void publishProgress() {
        progress = 0; // 0x000001fd9426812a
        var r10 = 8000; // 0x000001fd9426812e
        var r8 = 1; // 0x000001fd94268134

        do {
            var r11 = 99993; // 0x000001fd9426813a
            r11 -= r8; // 0x000001fd94268140
            if (8000 < r11) { // 0x000001fd94268143
                r11 = 8000; // 0x000001fd9426814a
            }

            r11 += r8; // 0x000001fd9426814e
            do {
                progress = r8; // 0x000001fd94268160
                var r9 = r8 + 7; // 0x000001fd94268164
                var ecx = r8 + 6; // 0x000001fd94268168
                var ebx = r8 + 1; // 0x000001fd9426816c
                progress = ebx; // 0x000001fd94268170
                ebx = r8 + 2; // 0x000001fd94268173
                progress = ebx; // 0x000001fd94268177
                ebx = r8 + 3; // 0x000001fd9426817a
                progress = ebx; // 0x000001fd9426817e
                ebx = r8 + 4;
                progress = ebx;
                ebx = r8 + 5;
                progress = ebx;
                progress = ecx;
                progress = r9;
                r8 += 8; // 0x000001fd94268196
            } while (r8 < r11);

            r11 = pollPageAddr;
            pollForSafepoint(r11); // 0x000001fd942681a3
        } while (r8 < 99993);

        do {
            progress = r8;
            r8++;
        } while (r8 < 100_000);
    }

    void pollForSafepoint(int addr) {

    }
}
