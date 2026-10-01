package com.ardencm.posttrade.confirms;

import java.time.LocalDate;

public record Confirmation(String tradeId, String mic, LocalDate tradeDate, LocalDate settlementDate,
                           LocalDate affirmationDeadline, String settlTypeTag63, String narrative) {}
