package com.ardencm.posttrade.dates;

import java.time.LocalDate;

/** Settlement-date arithmetic. All callers should go through this class rather than hard-coding lags. */
public final class SettlementDates {

    private SettlementDates() {}

    /** Contractual settlement date for a trade executed on {@code tradeDate} on market {@code mic}. */
    public static LocalDate settlementDate(LocalDate tradeDate, String mic) {
        return settlementDate(tradeDate, mic, SettlementCycle.forMarket(mic, tradeDate));
    }

    public static LocalDate settlementDate(LocalDate tradeDate, String mic, SettlementCycle cycle) {
        BusinessCalendar cal = BusinessCalendar.of(mic);
        LocalDate t = cal.isBusinessDay(tradeDate) ? tradeDate : cal.nextBusinessDay(tradeDate);
        return cal.addBusinessDays(t, cycle.businessDays());
    }

    /**
     * Latest date on which a settlement instruction can be affirmed and still settle on time.
     * Convention: settlement date minus one business day, before the CSD cut-off.
     */
    public static LocalDate affirmationDeadline(LocalDate settlementDate, String mic) {
        return BusinessCalendar.of(mic).addBusinessDays(settlementDate, -1);
    }
}
