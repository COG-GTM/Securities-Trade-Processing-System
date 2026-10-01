package com.ardencm.posttrade.dates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettlementDatesTest {

    @Nested
    class BeforeGoLive {
        @Test
        void londonTradeSettlesTwoBusinessDaysLater() {
            LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 6), "XLON");
            assertEquals(LocalDate.of(2027, 10, 8), sd);
        }

        @Test
        void lastTPlusTwoTradeDateSettlesOnTheFirstTPlusOneSettlementDate() {
            // Fri 8 Oct 2027 (T+2) and Mon 11 Oct 2027 (T+1) both settle Tue 12 Oct: the double-settlement day.
            assertEquals(LocalDate.of(2027, 10, 12), SettlementDates.settlementDate(LocalDate.of(2027, 10, 8), "XLON"));
            assertEquals(LocalDate.of(2027, 10, 12), SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XLON"));
        }

        @Test
        void londonBankHolidayIsSkipped() {
            // Friday 27 Aug 2027 -> Mon 30 Aug is a bank holiday -> Wed 1 Sep
            LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 8, 27), "XLON");
            assertEquals(LocalDate.of(2027, 9, 1), sd);
        }
    }

    @Nested
    class FromGoLive {
        @Test
        void londonTradeSettlesNextBusinessDay() {
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
        void fixTag63ForStandardCycleIsNextDay() {
            assertEquals("2", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 11))));
            assertEquals("3", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 8))));
        }
    }

    @Test
    void usMarketsAlreadyOnTPlusOne() {
        LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 6), "XNYS");
        assertEquals(LocalDate.of(2027, 10, 7), sd);
    }

    @Test
    void deprecatedNoArgFormsKeepPreGoLiveBehaviour() {
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.standard());
        assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XLON"));
    }
}
