package com.ardencm.posttrade.dates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formatting helpers for FIX 4.4 date fields. */
public final class FixDates {

    /** FIX LocalMktDate format used by tag 64 (SettlDate) and tag 75 (TradeDate). */
    private static final DateTimeFormatter LOCAL_MKT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FixDates() {}

    /** Tag 64 SettlDate value. */
    public static String tag64(LocalDate settlementDate) {
        return settlementDate.format(LOCAL_MKT_DATE);
    }

    /** Tag 75 TradeDate value. */
    public static String tag75(LocalDate tradeDate) {
        return tradeDate.format(LOCAL_MKT_DATE);
    }

    /**
     * Tag 63 SettlType. '0' = Regular (market standard cycle), '1' = Cash (T+0), '2' = Next day (T+1),
     * '3' = T+2, '4' = T+3.
     */
    public static String tag63(SettlementCycle cycle) {
        switch (cycle) {
            case T_PLUS_0:
                return "1";
            case T_PLUS_1:
                return "2";
            case T_PLUS_2:
                return "3";
            case T_PLUS_3:
                return "4";
            default:
                return "0";
        }
    }
}
