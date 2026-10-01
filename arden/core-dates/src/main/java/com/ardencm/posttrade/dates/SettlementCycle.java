package com.ardencm.posttrade.dates;

import java.time.LocalDate;

/**
 * Standard settlement cycle for cash equities and fixed income.
 *
 * <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland move on
 * {@link #EU_UK_CH_T1_GO_LIVE 11 October 2027}. The default cycle is therefore a function of the trade date:
 * trades dated before go-live settle T+2, trades dated on or after it settle T+1. Callers must pass the trade
 * date; the no-argument forms are kept only so existing binaries compile and are deprecated.
 */
public enum SettlementCycle {
    T_PLUS_0(0),
    T_PLUS_1(1),
    T_PLUS_2(2),
    T_PLUS_3(3);

    /** First trade date settling T+1 in EU/UK/CH markets (SETTLE-4471). */
    public static final LocalDate EU_UK_CH_T1_GO_LIVE = LocalDate.of(2027, 10, 11);

    private final int businessDays;

    SettlementCycle(int businessDays) {
        this.businessDays = businessDays;
    }

    public int businessDays() {
        return businessDays;
    }

    /** Default cycle for EU/UK/CH cash markets for a trade executed on {@code tradeDate}. */
    public static SettlementCycle standard(LocalDate tradeDate) {
        return tradeDate.isBefore(EU_UK_CH_T1_GO_LIVE) ? T_PLUS_2 : T_PLUS_1;
    }

    /**
     * @deprecated the standard cycle depends on the trade date since SETTLE-4471; use {@link #standard(LocalDate)}.
     *     Returns the pre-go-live value so existing callers keep their behaviour until migrated (tranche 2).
     */
    @Deprecated
    public static SettlementCycle standard() {
        return T_PLUS_2;
    }

    /** Resolves the standard cycle for a market identifier code (ISO 10383 MIC) and trade date. */
    public static SettlementCycle forMarket(String mic, LocalDate tradeDate) {
        switch (mic) {
            case "XNYS":
            case "XNAS":
            case "XTSE":
                return T_PLUS_1;
            default:
                return standard(tradeDate);
        }
    }

    /** @deprecated use {@link #forMarket(String, LocalDate)}. */
    @Deprecated
    public static SettlementCycle forMarket(String mic) {
        switch (mic) {
            case "XNYS":
            case "XNAS":
            case "XTSE":
                return T_PLUS_1;
            default:
                return standard();
        }
    }
}
