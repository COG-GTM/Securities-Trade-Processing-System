package com.ardencm.treasury.fxfunding;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxFundingServiceTest {
    private final FxFundingService service = new FxFundingService();

    @Test
    void parisSecuritiesAndFxSpotAlignUnderTPlusTwo() {
        FundingPlan p = service.plan("T-1", "EURGBP", "XPAR", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 13), p.securitiesSettlementDate());
        assertEquals(LocalDate.of(2027, 10, 13), p.fxSpotValueDate());
        assertFalse(p.requiresTomNext());
    }

    @Test
    void usSecuritiesAlreadyNeedTomNext() {
        FundingPlan p = service.plan("T-2", "USDEUR", "XNYS", LocalDate.of(2027, 10, 11));
        assertTrue(p.requiresTomNext());
        assertEquals(LocalDate.of(2027, 10, 11), p.fxDealDate());
    }
}
