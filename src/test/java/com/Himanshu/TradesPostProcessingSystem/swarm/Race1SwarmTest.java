package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.TradeConsumerService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** RACE persona: make one economic event settle twice or not at all. */
@Tag("swarm")
@SuppressWarnings({"unchecked", "rawtypes"})
class Race1SwarmTest {

    private static Trade trade(String tradeId, String symbol, int quantity, double price) {
        Trade t = new Trade();
        t.setTradeId(tradeId);
        t.setSymbol(symbol);
        t.setQuantity(quantity);
        t.setPrice(price);
        return t;
    }

    /** In-memory stand-in for the trades table: the unique constraint on trade_id is the only arbiter. */
    private static TradeRepository uniqueKeyTable(Map<String, TradeEntity> rows, AtomicInteger staleReads) {
        TradeRepository repo = mock(TradeRepository.class);
        when(repo.save(any(TradeEntity.class))).thenAnswer(inv -> {
            TradeEntity e = inv.getArgument(0);
            if (rows.putIfAbsent(e.getTradeId(), e) != null) {
                throw new DataIntegrityViolationException(
                        "ERROR: duplicate key value violates unique constraint \"uk_trades_trade_id\"");
            }
            return e;
        });
        when(repo.findByTradeId(anyString())).thenAnswer(inv ->
                staleReads.getAndDecrement() > 0 ? null : rows.get((String) inv.getArgument(0)));
        return repo;
    }

    private static TradeConsumerService consumerOver(TradeRepository repo) {
        TradeConsumerService consumer = new TradeConsumerService();
        ReflectionTestUtils.setField(consumer, "tradeRepository", repo);
        return consumer;
    }

    @Test
    void race1_redeliveredTradeIsBookedExactlyOnceAndIsNotAPoisonPill() {
        Map<String, TradeEntity> rows = new ConcurrentHashMap<>();
        TradeConsumerService consumer = consumerOver(uniqueKeyTable(rows, new AtomicInteger(0)));

        consumer.consumeTrade(trade("7f1c-replayed", "AAPL", 100, 150.50));
        // Kafka is at-least-once: a rebalance or a crash before the offset commit redelivers the same record.
        assertDoesNotThrow(() -> consumer.consumeTrade(trade("7f1c-replayed", "AAPL", 100, 150.50)),
                "redelivery of an already-booked trade must be an idempotent no-op, not a listener failure");
        assertEquals(1, rows.size(), "one economic event must map to exactly one booked row");
    }

    @Test
    void race1_concurrentRedeliveryThatLosesTheUniqueKeyRaceIsANoOp() {
        Map<String, TradeEntity> rows = new ConcurrentHashMap<>();
        // Two consumers (old and new partition owner during a rebalance) both read "not booked yet" before either inserts.
        TradeConsumerService consumer = consumerOver(uniqueKeyTable(rows, new AtomicInteger(2)));

        consumer.consumeTrade(trade("9a2e-rebalance", "MSFT", 50, 410.25));
        assertDoesNotThrow(() -> consumer.consumeTrade(trade("9a2e-rebalance", "MSFT", 50, 410.25)),
                "the check-then-act gap must be closed by treating a unique-key loss on an already-booked trade as a duplicate");
        assertEquals(1, rows.size());
    }

    @Test
    void guard_integrityFailureForATradeThatIsNotBookedStillFails() {
        TradeRepository repo = mock(TradeRepository.class);
        when(repo.save(any(TradeEntity.class))).thenThrow(new DataIntegrityViolationException("null value in column \"symbol\""));
        when(repo.findByTradeId(anyString())).thenReturn(null);
        TradeConsumerService consumer = consumerOver(repo);

        assertThrows(DataIntegrityViolationException.class, () -> consumer.consumeTrade(trade("c0de-unbooked", null, 1, 1.0)),
                "a write that failed for any reason other than 'already booked' must surface so the error handler retries it");
    }
}
