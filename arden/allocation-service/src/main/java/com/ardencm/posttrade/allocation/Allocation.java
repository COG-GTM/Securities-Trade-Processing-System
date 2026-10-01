package com.ardencm.posttrade.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Allocation(String blockId, String account, BigDecimal quantity, LocalDate tradeDate,
                         LocalDate settlementDate, String mic) {}
