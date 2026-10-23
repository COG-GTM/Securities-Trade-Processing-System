package com.ardencm.posttrade.dates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementDatesTest {

    @Test
    void londonTradeBeforeGoLiveSettlesTwoBusinessDaysLater() {
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 8), "XLON");
        assertEquals(LocalDate.of(2027, 10, 12), sd);
    }

    @Test
    void londonTradeFromGoLiveSettlesNextBusinessDay() {
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
    void goLiveGateAppliesToEveryInScopeVenue() {
        LocalDate before = LocalDate.of(2027, 10, 8);
        for (String mic : new String[] {"XLON", "XPAR", "XETR", "XSWX"}) {
            assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forTrade(mic, before), mic);
            assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forTrade(mic, SettlementCycle.EU_UK_CH_T1_GO_LIVE), mic);
        }
        assertEquals(SettlementCycle.T_PLUS_1, SettlementCycle.forTrade("XNYS", before));
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forTrade("XTKS", SettlementCycle.EU_UK_CH_T1_GO_LIVE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyEntryPointsKeepPreGoLiveCycle() {
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.standard());
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XLON"));
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
    @SuppressWarnings("deprecation")
    void fixTag63ForStandardCycleIsTPlusTwo() {
        assertEquals("3", FixDates.tag63(SettlementCycle.standard()));
    }

    @Test
    void fixTag63FollowsTheTradeDateGatedCycle() {
        assertEquals("3", FixDates.tag63(SettlementCycle.forTrade("XLON", LocalDate.of(2027, 10, 8))));
        assertEquals("2", FixDates.tag63(SettlementCycle.forTrade("XLON", LocalDate.of(2027, 10, 11))));
    }
}
