package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.FixDates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what this service assumes about core-dates so a future upgrade cannot silently change it.
 * core-dates 1.5.0 switched FixDates.tag64/tag75 to ISO-8601, which breaks the custody-gateway contract
 * (see CustodyGatewayContractTest); a core-dates bump must keep these assertions green.
 */
class CoreDatesWireFormatTest {

    @Test
    @DisplayName("core-dates: FIX tag 64 and tag 75 are LocalMktDate YYYYMMDD")
    void fixDatesAreLocalMktDate() {
        LocalDate date = LocalDate.of(2027, 1, 5);
        assertEquals("20270105", FixDates.tag64(date));
        assertEquals("20270105", FixDates.tag75(date));
    }

    @Test
    @DisplayName("core-dates: embedded snakeyaml is 2.x (CVE-2022-1471)")
    void embeddedSnakeyamlIsPatched() throws Exception {
        URL coreDatesPom = null;
        for (URL url : Collections.list(getClass().getClassLoader()
                .getResources("META-INF/maven/org.yaml/snakeyaml/pom.properties"))) {
            if (url.toString().contains("core-dates")) {
                coreDatesPom = url;
            }
        }
        assertNotNull(coreDatesPom, "core-dates jar no longer declares its embedded snakeyaml");
        Properties props = new Properties();
        try (InputStream in = coreDatesPom.openStream()) {
            props.load(in);
        }
        String version = props.getProperty("version");
        int major = Integer.parseInt(version.split("\\.")[0]);
        assertTrue(major >= 2, "core-dates embeds vulnerable snakeyaml " + version);
    }
}
