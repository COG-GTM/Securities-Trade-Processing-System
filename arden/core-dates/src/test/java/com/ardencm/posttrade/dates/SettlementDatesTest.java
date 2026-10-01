package com.ardencm.posttrade.dates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementDatesTest {

    @Test
    void londonTradeSettlesTwoBusinessDaysLater() {
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XLON");
        assertEquals(LocalDate.of(2027, 10, 13), sd);
    }

    @Test
    void weekendIsSkipped() {
        // Thursday trade -> Monday settlement under T+2
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 14), "XLON");
        assertEquals(LocalDate.of(2027, 10, 18), sd);
    }

    @Test
    void londonBankHolidayIsSkipped() {
        // Friday 27 Aug 2027 -> Mon 30 Aug is a bank holiday -> Wed 1 Sep
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 8, 27), "XLON");
        assertEquals(LocalDate.of(2027, 9, 1), sd);
    }

    @Test
    void usMarketsAlreadyOnTPlusOne() {
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XNYS");
        assertEquals(LocalDate.of(2027, 10, 12), sd);
    }

    @Test
    void fixTag64IsLocalMktDate() {
        assertEquals("20271013", FixDates.tag64(LocalDate.of(2027, 10, 13)));
    }

    @Test
    void fixTag63ForStandardCycleIsTPlusTwo() {
        assertEquals("3", FixDates.tag63(SettlementCycle.standard()));
    }
}
