package com.ardencm.posttrade.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record BlockTrade(String blockId, String mic, LocalDate tradeDate, BigDecimal quantity,
                         Map<String, BigDecimal> accountWeights, boolean giveUp) {}
