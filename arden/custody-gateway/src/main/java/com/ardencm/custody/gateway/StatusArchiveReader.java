package com.ardencm.custody.gateway;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Custodians deliver end-of-day MT548 batches as tar archives over SFTP. */
public final class StatusArchiveReader {
    private StatusArchiveReader() {}

    public static List<String> readMessages(InputStream tar) throws IOException {
        List<String> out = new ArrayList<>();
        try (TarArchiveInputStream in = new TarArchiveInputStream(tar)) {
            TarArchiveEntry entry;
            while ((entry = in.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    out.add(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return out;
    }
}
