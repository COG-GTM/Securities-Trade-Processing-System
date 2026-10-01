package com.ardencm.seclending.recall;

import com.ardencm.posttrade.dates.BusinessCalendar;
import com.ardencm.posttrade.dates.SettlementDates;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RecallService {

    /**
     * Standard recall notice period agreed in our GMSLA schedules: borrower returns within 2 business days.
     * Under T+2 a recall issued on trade date lands on settlement date; there is no slack.
     */
    static final int RECALL_NOTICE_BUSINESS_DAYS = 2;

    public RecallNotice recall(String loanId, String isin, String mic, LocalDate saleTradeDate) {
        LocalDate settle = SettlementDates.settlementDate(saleTradeDate, mic);
        BusinessCalendar cal = BusinessCalendar.of(mic);
        LocalDate deadline = cal.addBusinessDays(settle, -RECALL_NOTICE_BUSINESS_DAYS);
        return new RecallNotice(loanId, isin, saleTradeDate, settle, deadline, RECALL_NOTICE_BUSINESS_DAYS);
    }
}
