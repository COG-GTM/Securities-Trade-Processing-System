package com.ardencm.custody.gateway;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixSettlDateParserTest {
    @Test
    void parsesLocalMktDate() {
        assertEquals(LocalDate.of(2027, 10, 13), FixSettlDateParser.parse("20271013"));
    }

    @Test
    void rejectsIsoDates() {
        assertThrows(IllegalArgumentException.class, () -> FixSettlDateParser.parse("2027-10-13"));
    }
}
