package com.ardencm.treasury.fxfunding;

import com.ardencm.posttrade.dates.BusinessCalendar;
import com.ardencm.posttrade.dates.SettlementDates;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FxFundingService {

    /** FX spot convention: value date is T+2 for most pairs. This is an FX market convention, not CSDR. */
    private static final int FX_SPOT_DAYS = 2;

    public FundingPlan plan(String tradeId, String ccyPair, String mic, LocalDate tradeDate) {
        LocalDate secSettle = SettlementDates.settlementDate(tradeDate, mic);
        BusinessCalendar fxCal = BusinessCalendar.of("TARGET2");
        LocalDate spotValue = fxCal.addBusinessDays(tradeDate, FX_SPOT_DAYS);
        // If securities settle before spot value, treasury must deal tom-next (or pre-fund) instead.
        boolean tomNext = secSettle.isBefore(spotValue);
        LocalDate dealDate = tomNext ? fxCal.addBusinessDays(secSettle, -1) : tradeDate;
        return new FundingPlan(tradeId, ccyPair, secSettle, spotValue, dealDate, tomNext);
    }
}
