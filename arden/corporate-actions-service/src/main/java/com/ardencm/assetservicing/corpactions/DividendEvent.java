package com.ardencm.assetservicing.corpactions;

import java.time.LocalDate;

public record DividendEvent(String eventId, String isin, String primaryMic, LocalDate recordDate,
                            LocalDate exDate, LocalDate paymentDate, String calendarUsed) {}
