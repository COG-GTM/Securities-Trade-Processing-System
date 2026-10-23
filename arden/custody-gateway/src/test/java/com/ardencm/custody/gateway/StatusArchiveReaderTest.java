package com.ardencm.custody.gateway;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins how custodian MT548 tar drops are ingested, so a commons-compress upgrade cannot silently change it.
 * Fixtures are produced by GNU tar (see src/test/resources/mt548-archives/README.md), not by commons-compress.
 */
class StatusArchiveReaderTest {
    private static final String EUROCLEAR_0001 =
            "{1:F01ARDNGB2LAXXX0000000000}{2:O548EUROCLEARXXX}{4:\r\n:20C::SEME//MT548-0001\r\n:25D::MTCH//MACH\r\n-}";
    private static final String CLEARSTREAM_0002 =
            "{1:F01ARDNGB2LAXXX0000000000}{2:O548CEDELULLXXX}{4:\r\n:20C::SEME//MT548-0002\r\n:24B::NMAT//CMIS\r\n:70D::REAS//Z\u00fcrich\r\n-}";

    @Test
    void readsUstarEntriesInArchiveOrderSkippingDirectories() throws IOException {
        assertEquals(List.of(EUROCLEAR_0001, CLEARSTREAM_0002), read("ustar-two-messages.tar"));
    }

    @Test
    void readsGnuLongFileNameEntries() throws IOException {
        assertEquals(List.of(EUROCLEAR_0001), read("gnu-longname.tar"));
    }

    @Test
    void readsPaxLongFileNameEntries() throws IOException {
        assertEquals(List.of(EUROCLEAR_0001), read("pax-longname.tar"));
    }

    @Test
    void emptyStreamYieldsNoMessages() throws IOException {
        assertEquals(List.of(), StatusArchiveReader.readMessages(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void truncatedDropFailsInsteadOfReturningPartialBatch() throws IOException {
        byte[] tar = bytes("ustar-two-messages.tar");
        byte[] truncated = Arrays.copyOf(tar, 512 * 4 + 40);

        assertThrows(IOException.class, () -> StatusArchiveReader.readMessages(new ByteArrayInputStream(truncated)));
    }

    private static List<String> read(String fixture) throws IOException {
        return StatusArchiveReader.readMessages(new ByteArrayInputStream(bytes(fixture)));
    }

    private static byte[] bytes(String fixture) throws IOException {
        try (InputStream in = StatusArchiveReaderTest.class.getResourceAsStream("/mt548-archives/" + fixture)) {
            if (in == null) throw new IOException("missing fixture " + fixture);
            return in.readAllBytes();
        }
    }
}
