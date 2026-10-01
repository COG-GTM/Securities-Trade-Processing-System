package com.ardencm.posttrade.confirms;

import com.ardencm.posttrade.dates.FixDates;
import com.ardencm.posttrade.dates.SettlementCycle;
import com.ardencm.posttrade.dates.SettlementDates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ConfirmationService {

    private final String narrativeTemplate;

    public ConfirmationService(@Value("${confirms.narrative}") String narrativeTemplate) {
        this.narrativeTemplate = narrativeTemplate;
    }

    public Confirmation confirm(String tradeId, String mic, LocalDate tradeDate) {
        LocalDate settlement = SettlementDates.settlementDate(tradeDate, mic);
        LocalDate deadline = SettlementDates.affirmationDeadline(settlement, mic);
        // All non-US confirms are regular-way T+2; US moved in May 2024.
        SettlementCycle cycle = mic.startsWith("XN") ? SettlementCycle.T_PLUS_1 : SettlementCycle.T_PLUS_2;
        return new Confirmation(tradeId, mic, tradeDate, settlement, deadline, FixDates.tag63(cycle),
                narrativeTemplate.replace("{settlement}", settlement.toString()));
    }
}
