package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.FixDates;
import com.ardencm.posttrade.dates.SettlementCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the wire formats this service takes from core-dates. core-dates 1.5.0 switched FixDates.tag64/tag75
 * to ISO-8601, which custodians reject in FIX 4.4; any core-dates upgrade must keep these assertions green.
 */
class CoreDatesWireFormatTest {

    private static final LocalDate DATE = LocalDate.of(2027, 1, 5);

    @Test
    @DisplayName("core-dates FixDates.tag64 and tag75 emit FIX LocalMktDate (yyyyMMdd)")
    void fixDatesEmitLocalMktDate() {
        assertEquals("20270105", FixDates.tag64(DATE));
        assertEquals("20270105", FixDates.tag75(DATE));
    }

    @Test
    @DisplayName("FIX message carries LocalMktDate in tags 64 and 75; sese.023 SttlmDt stays ISO-8601")
    void renderedWireFormats() {
        SettlementCycleConfig cfg = new SettlementCycleConfig();
        cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));
        InstructionBuilder b = new InstructionBuilder(cfg);
        Instruction i = b.build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));

        String fix = b.toFix(i);
        assertTrue(fix.contains("\u000175=20271011\u0001"), fix);
        assertTrue(fix.contains("\u000164=20271013\u0001"), fix);
        assertTrue(b.toSese023(i).contains("<SttlmDt><Dt><Dt>2027-10-13</Dt></Dt></SttlmDt>"), b.toSese023(i));
    }
}
