package com.ardencm.custody.gateway;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Pins the custodian MT548 tar drop format that StatusArchiveReader consumes, so a commons-compress upgrade
 * cannot silently change what we ingest. The fixture was produced by GNU tar (--format=gnu, includes a
 * ././@LongLink entry for a >100 char member name), not by commons-compress, so it reflects what custodians send.
 */
class StatusArchiveReaderContractTest {

    private static final String FIXTURE = "/mt548/eod-gnu-tar.tar";

    private static byte[] fixture() throws IOException {
        try (InputStream in = StatusArchiveReaderContractTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "missing fixture " + FIXTURE);
            return in.readAllBytes();
        }
    }

    private static String mt548(String seme) {
        return ":16R:GENL\r\n:20C::SEME//" + seme + "\r\n:23G:INST\r\n:16S:GENL\r\n";
    }

    @Test
    void readsGnuTarDropInArchiveOrderSkippingDirectories() throws IOException {
        List<String> messages = StatusArchiveReader.readMessages(new ByteArrayInputStream(fixture()));
        assertEquals(List.of(mt548("CUST-0001"), mt548("CUST-0002"), mt548("CUST-0003")), messages);
    }

    @Test
    void truncatedArchiveTerminates() {
        byte[] full;
        try {
            full = fixture();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        byte[] truncated = Arrays.copyOf(full, 1600);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            try {
                StatusArchiveReader.readMessages(new ByteArrayInputStream(truncated));
            } catch (IOException expected) {
                // rejecting a corrupt drop is acceptable; hanging is not
            }
        });
    }
}
