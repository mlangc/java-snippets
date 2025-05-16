package at.mlangc.concurrent.seqcst.vs.ackrel;

import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;

class TestState {
    private final CRC32 crc32 = new CRC32();

    private final long[] data = new long[64];
    private long checksum;

    TestState() {
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }

        checksum = calcChecksum();
    }

    void update() {
        for (int i = 0; i < data.length; i++) {
            data[i]++;
        }

        checksum = calcChecksum();
    }

    void assertConsistent() {
        for (int i = 1; i < data.length; i++) {
            assertThat(data[i]).isEqualTo(data[i - 1] + 1);
        }

        assertThat(data[0]).isNotNegative();
        assertThat(checksum).isEqualTo(calcChecksum());
    }

    void assertConsistent(long expectedUpdates) {
        assertThat(data[0]).isEqualTo(expectedUpdates);
        assertConsistent();
    }

    private long calcChecksum() {
        crc32.reset();
        for (long l : data) {
            for (int b = 0; b < Long.BYTES; b++) {
                crc32.update((int) ((0xff) & (l << b)));
            }
        }

        return crc32.getValue();
    }
}
