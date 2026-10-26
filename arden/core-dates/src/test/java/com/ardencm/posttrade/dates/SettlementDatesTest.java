package com.ardencm.posttrade.dates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementDatesTest {

    @Test
    void londonTradeBeforeGoLiveSettlesTwoBusinessDaysLater() {
        // Friday 8 Oct 2027 -> Tuesday 12 Oct under T+2
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 8), "XLON");
        assertEquals(LocalDate.of(2027, 10, 12), sd);
    }

    @Test
    void londonTradeOnGoLiveSettlesNextBusinessDay() {
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XLON");
        assertEquals(LocalDate.of(2027, 10, 12), sd);
    }

    @Test
    void weekendIsSkipped() {
        // Friday trade -> Monday settlement under T+1
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 15), "XLON");
        assertEquals(LocalDate.of(2027, 10, 18), sd);
    }

    @Test
    void euUkChCycleIsGatedOnTradeDate() {
        for (String mic : new String[] {"XLON", "XPAR", "XETR", "XSWX"}) {
            assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket(mic, LocalDate.of(2027, 10, 10)), mic);
            assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forMarket(mic, LocalDate.of(2027, 10, 11)), mic);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyEntryPointsKeepPreGoLiveCycle() {
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.standard());
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XLON"));
        assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forMarket("XNYS"));
    }

    @Test
    void marketsOutsideScopeAreUnchanged() {
        assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forMarket("XNYS", LocalDate.of(2027, 9, 1)));
        assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forMarket("XTSE", LocalDate.of(2027, 11, 1)));
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XTKS", LocalDate.of(2027, 11, 1)));
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
    void fixTag63FollowsTheCycle() {
        assertEquals("3", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 8))));
        assertEquals("2", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 11))));
    }
}
