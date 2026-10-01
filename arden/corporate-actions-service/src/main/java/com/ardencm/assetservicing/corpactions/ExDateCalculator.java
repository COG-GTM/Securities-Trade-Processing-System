package com.ardencm.assetservicing.corpactions;

import com.ardencm.posttrade.dates.BusinessCalendar;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ExDateCalculator {

    /**
     * Ex-date = record date minus (settlement cycle - 1) business days. Under T+2 that is one business day
     * before record date. Under T+1 ex-date and record date coincide (as in the US since May 2024).
     */
    private static final int EX_DATE_OFFSET_BUSINESS_DAYS = 1;

    public DividendEvent derive(String eventId, String isin, String primaryMic, LocalDate recordDate, LocalDate paymentDate) {
        // Entitlement is settled at the CSD, so we use the payment-system calendar rather than the venue calendar.
        String calendar = primaryMic.equals("XLON") ? "XLON" : "TARGET2";
        BusinessCalendar cal = BusinessCalendar.of(calendar);
        LocalDate exDate = cal.addBusinessDays(recordDate, -EX_DATE_OFFSET_BUSINESS_DAYS);
        return new DividendEvent(eventId, isin, primaryMic, recordDate, exDate, paymentDate, calendar);
    }
}
