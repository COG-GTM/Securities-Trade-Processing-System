package com.ardencm.settlements.instruction;

import java.time.LocalDate;

public record Instruction(String tradeId, String isin, String mic, LocalDate tradeDate, LocalDate settlementDate,
                          String fixSettlDate, String fixSettlType, String iso20022SttlmDt) {}
