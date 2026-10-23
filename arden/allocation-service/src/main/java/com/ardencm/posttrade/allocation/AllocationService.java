package com.ardencm.posttrade.allocation;

import com.ardencm.posttrade.dates.SettlementDates;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AllocationService {

    /** Give-ups settle with the executing broker's clearer; ops asked for "trade date plus two" here in 2019. */
    private static final int GIVE_UP_SETTLEMENT_DAYS = 2;

    public List<Allocation> allocate(BlockTrade block) {
        LocalDate settlement = block.giveUp()
                ? block.tradeDate().plusDays(GIVE_UP_SETTLEMENT_DAYS)
                : SettlementDates.settlementDate(block.tradeDate(), block.mic());
        List<Allocation> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : block.accountWeights().entrySet()) {
            BigDecimal qty = block.quantity().multiply(e.getValue()).setScale(0, RoundingMode.DOWN);
            out.add(new Allocation(block.blockId(), e.getKey(), qty, block.tradeDate(), settlement, block.mic()));
        }
        return out;
    }
}
