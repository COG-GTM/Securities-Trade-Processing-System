package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.FixDates;
import com.ardencm.posttrade.dates.SettlementCycle;
import com.ardencm.posttrade.dates.SettlementDates;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class InstructionBuilder {
    private final SettlementCycleConfig config;

    public InstructionBuilder(SettlementCycleConfig config) {
        this.config = config;
    }

    public Instruction build(String tradeId, String isin, String mic, LocalDate tradeDate) {
        SettlementCycle cycle = config.cycleFor(mic, tradeDate);
        LocalDate settlement = SettlementDates.settlementDate(tradeDate, mic, cycle);
        return new Instruction(tradeId, isin, mic, tradeDate, settlement,
                FixDates.tag64(settlement), FixDates.tag63(cycle), settlement.toString());
    }

    /** FIX 4.4 tag=value rendering. Custodians parse tag 64 as LocalMktDate (YYYYMMDD). */
    public String toFix(Instruction i) {
        return String.join("\u0001",
                "8=FIX.4.4", "35=AE", "571=" + i.tradeId(), "48=" + i.isin(), "22=4",
                "75=" + FixDates.tag75(i.tradeDate()), "63=" + i.fixSettlType(), "64=" + i.fixSettlDate(),
                "207=" + i.mic()) + "\u0001";
    }

    public String toSese023(Instruction i) {
        return "<SctiesSttlmTxInstr><TradDtls><TradDt><Dt><Dt>" + i.tradeDate() + "</Dt></Dt></TradDt>"
                + "<SttlmDt><Dt><Dt>" + i.iso20022SttlmDt() + "</Dt></Dt></SttlmDt></TradDtls>"
                + "<FinInstrmId><ISIN>" + i.isin() + "</ISIN></FinInstrmId></SctiesSttlmTxInstr>";
    }
}
