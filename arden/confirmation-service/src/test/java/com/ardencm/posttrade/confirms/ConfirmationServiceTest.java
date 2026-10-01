package com.ardencm.posttrade.confirms;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationServiceTest {
    private final ConfirmationService service =
            new ConfirmationService("Settles {settlement} on a T+2 basis.");

    @Test
    void parisTradeConfirmsTPlusTwo() {
        Confirmation c = service.confirm("T-1", "XPAR", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 13), c.settlementDate());
        assertEquals(LocalDate.of(2027, 10, 12), c.affirmationDeadline());
        assertEquals("3", c.settlTypeTag63());
        assertTrue(c.narrative().contains("2027-10-13"));
    }

    @Test
    void nyseTradeConfirmsTPlusOne() {
        Confirmation c = service.confirm("T-2", "XNYS", LocalDate.of(2027, 10, 11));
        assertEquals("2", c.settlTypeTag63());
    }
}
