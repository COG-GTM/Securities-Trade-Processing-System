package com.ardencm.seclending.recall;

import java.time.LocalDate;

public record RecallNotice(String loanId, String isin, LocalDate saleTradeDate, LocalDate saleSettlementDate,
                           LocalDate recallDeadline, int recallWindowBusinessDays) {}
