package com.ardencm.posttrade.allocation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationServiceTest {
    private final AllocationService service = new AllocationService();

    @Test
    void londonBlockSettlesTwoBusinessDaysLater() {
        BlockTrade block = new BlockTrade("BLK-1", "XLON", LocalDate.of(2027, 10, 11), new BigDecimal("10000"),
                Map.of("ACC-A", new BigDecimal("0.6"), "ACC-B", new BigDecimal("0.4")), false);
        List<Allocation> out = service.allocate(block);
        assertEquals(2, out.size());
        assertEquals(LocalDate.of(2027, 10, 13), out.get(0).settlementDate());
    }

    @Test
    void giveUpUsesCalendarDays() {
        BlockTrade block = new BlockTrade("BLK-2", "XLON", LocalDate.of(2027, 10, 8), new BigDecimal("100"),
                Map.of("ACC-A", BigDecimal.ONE), true);
        assertEquals(LocalDate.of(2027, 10, 10), service.allocate(block).get(0).settlementDate());
    }
}
