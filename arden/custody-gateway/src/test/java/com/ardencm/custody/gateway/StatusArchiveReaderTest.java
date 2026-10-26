package com.ardencm.custody.gateway;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** mt548-batch.tar is built with GNU tar 1.34 (--format=gnu), the format custodian SFTP drops use. */
class StatusArchiveReaderTest {

    @Test
    void readsGnuTarBatchSkippingDirectories() throws IOException {
        try (InputStream tar = fixture()) {
            List<String> messages = StatusArchiveReader.readMessages(tar);
            assertEquals(2, messages.size());
            assertTrue(messages.get(0).contains(":20C::SEME//MT548-0001"));
            assertTrue(messages.get(1).contains(":25D::SETT//PEND"));
        }
    }

    @Test
    void truncatedArchiveFailsFastInsteadOfHanging() throws IOException {
        byte[] full;
        try (InputStream tar = fixture()) {
            full = tar.readAllBytes();
        }
        byte[] truncated = Arrays.copyOf(full, 512 + 512 + 30);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(IOException.class,
                        () -> StatusArchiveReader.readMessages(new ByteArrayInputStream(truncated))));
    }

    @Test
    void commonsCompressIsAtLeastTheCve202425710FixVersion() {
        String version = TarArchiveInputStream.class.getPackage().getImplementationVersion();
        assertNotNull(version, "commons-compress manifest has no Implementation-Version");
        int[] v = Arrays.stream(version.split("[.-]")).limit(2).mapToInt(Integer::parseInt).toArray();
        assertTrue(v[0] > 1 || (v[0] == 1 && v[1] >= 26),
                "commons-compress " + version + " is below 1.26.0 (CVE-2024-25710)");
    }

    private static InputStream fixture() {
        InputStream in = StatusArchiveReaderTest.class.getResourceAsStream("/mt548-batch.tar");
        assertNotNull(in, "mt548-batch.tar fixture missing");
        return in;
    }
}
