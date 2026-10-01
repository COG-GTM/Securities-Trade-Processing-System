package com.ardencm.treasury.fxfunding;

import java.time.LocalDate;

public record FundingPlan(String tradeId, String ccyPair, LocalDate securitiesSettlementDate,
                          LocalDate fxSpotValueDate, LocalDate fxDealDate, boolean requiresTomNext) {}
