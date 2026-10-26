package com.ardencm.posttrade.dates;

import java.time.LocalDate;
import java.util.Set;

/**
 * Standard settlement cycle for cash equities and fixed income.
 *
 * <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland remain on T+2 and are
 * move on 11 October 2027 (SETTLE-4471): {@link #forMarket(String, LocalDate)} returns {@link #T_PLUS_1}
 * for EU/UK/CH regular-way trades dated on or after {@link #EU_UK_CH_T1_GO_LIVE} and {@link #T_PLUS_2} before it.
 */
public enum SettlementCycle {
    T_PLUS_0(0),
    T_PLUS_1(1),
    T_PLUS_2(2),
    T_PLUS_3(3);

    private final int businessDays;

    SettlementCycle(int businessDays) {
        this.businessDays = businessDays;
    }

    public int businessDays() {
        return businessDays;
    }

    /** First trade date settling T+1 in EU/UK/CH regular-way markets (SETTLE-4471). */
    public static final LocalDate EU_UK_CH_T1_GO_LIVE = LocalDate.of(2027, 10, 11);

    /** EU/UK/CH venues in scope of SETTLE-4471. */
    static final Set<String> EU_UK_CH_MICS = Set.of("XLON", "XPAR", "XETR", "XSWX");

    /**
     * Pre-go-live default cycle for EU/UK/CH cash markets.
     *
     * @deprecated has no trade date, so always returns {@link #T_PLUS_2}; use {@link #standard(LocalDate)}.
     */
    @Deprecated
    public static SettlementCycle standard() {
        return T_PLUS_2;
    }

    /** Default cycle for an EU/UK/CH cash trade executed on {@code tradeDate}. */
    public static SettlementCycle standard(LocalDate tradeDate) {
        return tradeDate.isBefore(EU_UK_CH_T1_GO_LIVE) ? T_PLUS_2 : T_PLUS_1;
    }

    /**
     * Pre-go-live cycle for a market identifier code (ISO 10383 MIC).
     *
     * @deprecated has no trade date, so EU/UK/CH always resolve to {@link #T_PLUS_2}; use {@link #forMarket(String, LocalDate)}.
     */
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

    /** Standard cycle for a trade executed on {@code tradeDate} on market {@code mic}. Other markets are unchanged. */
    public static SettlementCycle forMarket(String mic, LocalDate tradeDate) {
        if (EU_UK_CH_MICS.contains(mic)) {
            return standard(tradeDate);
        }
        return forMarket(mic);
    }
}
