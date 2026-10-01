package com.ardencm.assetservicing.corpactions;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExDateCalculatorTest {
    private final ExDateCalculator calc = new ExDateCalculator();

    @Test
    void exDateIsOneBusinessDayBeforeRecordUnderTPlusTwo() {
        DividendEvent e = calc.derive("DIV-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 15), LocalDate.of(2027, 11, 5));
        assertEquals(LocalDate.of(2027, 10, 14), e.exDate());
    }

    @Test
    void swissNameUsesTarget2Calendar() {
        DividendEvent e = calc.derive("DIV-2", "CH0012032048", "XSWX", LocalDate.of(2027, 5, 18), LocalDate.of(2027, 5, 25));
        // 17 May 2027 is Whit Monday: TARGET2 open, XSWX closed. We currently follow TARGET2.
        assertEquals(LocalDate.of(2027, 5, 17), e.exDate());
        assertEquals("TARGET2", e.calendarUsed());
    }
}
