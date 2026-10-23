package com.ardencm.posttrade.dates;

import java.time.LocalDate;
import java.util.Set;

/**
 * Standard settlement cycle for cash equities and fixed income.
 *
 * <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland move from T+2 to T+1 for trades dated on or
 * after {@link #EU_UK_CH_T1_GO_LIVE} (SETTLE-4471). Use {@link #forTrade(String, LocalDate)}; the undated
 * {@link #standard()} and {@link #forMarket(String)} keep returning the pre-go-live cycle.
 */
public enum SettlementCycle {
    T_PLUS_0(0),
    T_PLUS_1(1),
    T_PLUS_2(2),
    T_PLUS_3(3);

    /** First trade date settling T+1 on EU/UK/CH venues (SETTLE-4471). */
    public static final LocalDate EU_UK_CH_T1_GO_LIVE = LocalDate.of(2027, 10, 11);

    private static final Set<String> NORTH_AMERICA = Set.of("XNYS", "XNAS", "XTSE");
    private static final Set<String> EU_UK_CH = Set.of("XLON", "XPAR", "XETR", "XSWX");

    private final int businessDays;

    SettlementCycle(int businessDays) {
        this.businessDays = businessDays;
    }

    public int businessDays() {
        return businessDays;
    }

    /**
     * Default cycle for EU/UK/CH cash markets before {@link #EU_UK_CH_T1_GO_LIVE}.
     *
     * @deprecated not trade-date aware; use {@link #forTrade(String, LocalDate)}.
     */
    @Deprecated
    public static SettlementCycle standard() {
        return T_PLUS_2;
    }

    /**
     * Resolves the pre-go-live standard cycle for a market identifier code (ISO 10383 MIC).
     *
     * @deprecated not trade-date aware; use {@link #forTrade(String, LocalDate)}.
     */
    @Deprecated
    public static SettlementCycle forMarket(String mic) {
        return NORTH_AMERICA.contains(mic) ? T_PLUS_1 : standard();
    }

    /** Standard cycle for a trade executed on {@code tradeDate} on market {@code mic}. */
    public static SettlementCycle forTrade(String mic, LocalDate tradeDate) {
        if (EU_UK_CH.contains(mic) && !tradeDate.isBefore(EU_UK_CH_T1_GO_LIVE)) {
            return T_PLUS_1;
        }
        return forMarket(mic);
    }
}
