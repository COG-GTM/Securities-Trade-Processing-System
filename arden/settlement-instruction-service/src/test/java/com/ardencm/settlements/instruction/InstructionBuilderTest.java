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
        cfg.setCycles(Map.of("XNYS", SettlementCycle.T_PLUS_1));
        return new InstructionBuilder(cfg);
    }

    @Test
    void londonInstructionBeforeGoLiveSettlesTPlusTwo() {
        Instruction i = builder().build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 8));
        assertEquals(LocalDate.of(2027, 10, 12), i.settlementDate());
        assertEquals("20271012", i.fixSettlDate());
        assertEquals("3", i.fixSettlType());
    }

    @Test
    void londonInstructionFromGoLiveSettlesTPlusOne() {
        Instruction i = builder().build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 12), i.settlementDate());
        assertEquals("20271012", i.fixSettlDate());
        assertEquals("2", i.fixSettlType());
    }

    @Test
    void explicitPinStillWins() {
        SettlementCycleConfig cfg = new SettlementCycleConfig();
        cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));
        Instruction i = new InstructionBuilder(cfg).build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 13), i.settlementDate());
        assertEquals("3", i.fixSettlType());
    }

    @Test
    void fixMessageCarriesLocalMktDate() {
        InstructionBuilder b = builder();
        String fix = b.toFix(b.build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11)));
        assertTrue(fix.contains("\u000175=20271011\u0001"), fix);
        assertTrue(fix.contains("\u000163=2\u0001"), fix);
        assertTrue(fix.contains("\u000164=20271012\u0001"), fix);
    }
}
