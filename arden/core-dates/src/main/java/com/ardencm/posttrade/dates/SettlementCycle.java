package com.ardencm.posttrade.dates;

/**
 * Standard settlement cycle for cash equities and fixed income.
 *
 * <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland remain on T+2 and are
 * scheduled to move on 11 October 2027. Until then {@link #standard()} returns {@link #T_PLUS_2}
 * for every non-North-American market.
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

    /** Default cycle for EU/UK/CH cash markets. */
    public static SettlementCycle standard() {
        return T_PLUS_2;
    }

    /** Resolves the standard cycle for a market identifier code (ISO 10383 MIC). */
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
