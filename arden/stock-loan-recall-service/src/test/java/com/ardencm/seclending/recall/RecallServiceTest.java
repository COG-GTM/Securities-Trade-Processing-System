package com.ardencm.seclending.recall;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecallServiceTest {
    @Test
    void recallDeadlineIsTradeDateUnderTPlusTwo() {
        RecallNotice n = new RecallService().recall("L-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
        assertEquals(LocalDate.of(2027, 10, 13), n.saleSettlementDate());
        assertEquals(LocalDate.of(2027, 10, 11), n.recallDeadline());
    }
}
