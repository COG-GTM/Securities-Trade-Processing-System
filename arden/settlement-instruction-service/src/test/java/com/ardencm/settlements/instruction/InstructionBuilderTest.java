package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.SettlementCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionBuilderTest {
    private InstructionBuilder builder() {
        SettlementCycleConfig cfg = new SettlementCycleConfig();
        cfg.setCycles(Map.of("XNYS", SettlementCycle.T_PLUS_1, "XLON", SettlementCycle.T_PLUS_2));
        return new InstructionBuilder(cfg);
    }

    @Test
    void londonInstructionSettlesTPlusTwo() {
        Instruction i = builder().build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 13), i.settlementDate());
        assertEquals("20271013", i.fixSettlDate());
        assertEquals("3", i.fixSettlType());
    }

    @Test
    void fixMessageCarriesLocalMktDate() {
        InstructionBuilder b = builder();
        String fix = b.toFix(b.build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11)));
        assertTrue(fix.contains("\u000164=20271013\u0001"), fix);
    }
}
