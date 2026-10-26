package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.SettlementCycle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-venue settlement cycle overrides. Anything not listed falls back to {@link SettlementCycle#standard(LocalDate)}.
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

    /** Explicit venue pins win; everything else follows the date-gated market default (T+2 → T+1 on 11 Oct 2027). */
    public SettlementCycle cycleFor(String mic, LocalDate tradeDate) {
        return cycles.getOrDefault(mic, SettlementCycle.standard(tradeDate));
    }
}
