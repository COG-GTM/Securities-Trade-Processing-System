package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.FixDates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what we rely on from core-dates. core-dates 1.5.0 switched FixDates.tag64/tag75 to ISO-8601, which
 * custody-gateway rejects (see CustodyGatewayContractTest). A core-dates upgrade that breaks this test needs a
 * conversion in InstructionBuilder and a contract bump with @ardencm/custody-integration first.
 */
class CoreDatesWireFormatTest {

    @Test
    @DisplayName("core-dates FixDates.tag64 and tag75 emit FIX LocalMktDate (yyyyMMdd)")
    void fixDatesEmitLocalMktDate() {
        LocalDate date = LocalDate.of(2027, 9, 1);
        assertEquals("20270901", FixDates.tag64(date));
        assertEquals("20270901", FixDates.tag75(date));
    }

    @Test
    @DisplayName("core-dates embeds snakeyaml 2.x or later (CVE-2022-1471)")
    void embeddedSnakeyamlIsNotVulnerable() throws Exception {
        try (InputStream in = FixDates.class.getResourceAsStream("/META-INF/maven/org.yaml/snakeyaml/pom.properties")) {
            assertNotNull(in, "embedded snakeyaml pom.properties missing from core-dates");
            Properties p = new Properties();
            p.load(in);
            String version = p.getProperty("version");
            int major = Integer.parseInt(version.split("\\.")[0]);
            assertTrue(major >= 2, "core-dates embeds snakeyaml " + version + "; CVE-2022-1471 is fixed in 2.0");
        }
    }
}
