package com.ardencm.settlements.instruction;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ardencm.posttrade.dates.FixDates;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pins the FIX tag 64 wire format produced by core-dates. custody-gateway parses tag 64 as LocalMktDate (yyyyMMdd);
 * core-dates 1.5.0 changed FixDates.tag64 to ISO-8601 and broke the consumer contract. Any core-dates upgrade that
 * changes this format must be coordinated with @ardencm/custody-integration (ADR 0007, common-java-bom).
 */
class Tag64WireFormatPinTest {
    @Test
    void tag64StaysLocalMktDate() {
        String tag64 = FixDates.tag64(LocalDate.of(2027, 10, 13));
        assertTrue(tag64.matches("^[0-9]{8}$"), "tag 64 must be yyyyMMdd, got " + tag64);
    }
}
