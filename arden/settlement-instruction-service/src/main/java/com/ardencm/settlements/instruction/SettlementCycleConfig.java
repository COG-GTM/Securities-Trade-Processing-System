package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.SettlementCycle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-venue settlement cycle overrides. Anything not listed falls back to the core-dates market default for the
 * trade date ({@link SettlementCycle#forMarket(String, LocalDate)}).
 * Owned by settlements-core; changes need market-ops sign-off (see OPS-RUNBOOK-14).
 */
@Configuration
@ConfigurationProperties(prefix = "settlement")
public class SettlementCycleConfig {
    private Map<String, SettlementCycle> cycles = new HashMap<>();

    public Map<String, SettlementCycle> getCycles() {
        return cycles;
    }

    public void setCycles(Map<String, SettlementCycle> cycles) {
        this.cycles = cycles;
    }

    /** @deprecated has no trade date, so unlisted venues fall back to the pre-go-live cycle; use {@link #cycleFor(String, LocalDate)}. */
    @Deprecated
    @SuppressWarnings("deprecation")
    public SettlementCycle cycleFor(String mic) {
        return cycles.getOrDefault(mic, SettlementCycle.standard());
    }

    public SettlementCycle cycleFor(String mic, LocalDate tradeDate) {
        SettlementCycle pinned = cycles.get(mic);
        return pinned != null ? pinned : SettlementCycle.forMarket(mic, tradeDate);
    }
}
