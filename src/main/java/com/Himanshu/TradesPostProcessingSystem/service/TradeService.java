package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TradeService {

    private static final String TOPIC = "trades";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int MAX_REMEMBERED_KEYS = 10_000;

    private final Map<String, Trade> tradesByIdempotencyKey = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Trade> eldest) {
            return size() > MAX_REMEMBERED_KEYS;
        }
    };

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String symbol, int quantity, double price) {
        return generateAndSendTrade(symbol, quantity, price, null);
    }

    public String generateAndSendTrade(String symbol, int quantity, double price, String idempotencyKey) {
        // ✅ Trade Validation
        if (symbol == null || symbol.trim().isEmpty()) {
            return "❌ Error: Stock symbol cannot be empty!";
        }
        if (quantity <= 0) {
            return "❌ Error: Quantity must be greater than zero!";
        }
        if (price <= 0) {
            return "❌ Error: Price must be greater than zero!";
        }

        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            return "❌ Error: Idempotency-Key must be 1 to " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters!";
        }

        // ✅ Create Trade Object
        Trade trade = new Trade(symbol, quantity, price);

        if (idempotencyKey != null) {
            // Same key always yields the same tradeId, so a retry that reaches another instance is still
            // collapsed downstream by the unique trade_id constraint.
            trade.setTradeId(UUID.nameUUIDFromBytes(("trade:" + idempotencyKey).getBytes(StandardCharsets.UTF_8)).toString());
            Trade accepted;
            synchronized (tradesByIdempotencyKey) {
                accepted = tradesByIdempotencyKey.putIfAbsent(idempotencyKey, trade);
            }
            if (accepted != null) {
                if (!sameEconomics(accepted, trade)) {
                    return "❌ Error: Idempotency-Key was already used for a different trade!";
                }
                return "✅ Trade already accepted: " + accepted;
            }
        }

        // ✅ Send Trade to Kafka; a key whose publish fails is released so an identical retry publishes again
        CompletableFuture<SendResult<String, Trade>> sent;
        try {
            sent = kafkaTemplate.send(TOPIC, trade.getTradeId(), trade);
        } catch (RuntimeException e) {
            forget(idempotencyKey, trade);
            throw e;
        }
        if (idempotencyKey != null) {
            sent.whenComplete((result, failure) -> {
                if (failure != null) {
                    forget(idempotencyKey, trade);
                }
            });
        }
        return "✅ Trade sent successfully: " + trade;
    }

    private void forget(String idempotencyKey, Trade trade) {
        if (idempotencyKey != null) {
            synchronized (tradesByIdempotencyKey) {
                tradesByIdempotencyKey.remove(idempotencyKey, trade);
            }
        }
    }

    private static boolean sameEconomics(Trade a, Trade b) {
        return a.getSymbol().equals(b.getSymbol())
                && a.getQuantity() == b.getQuantity()
                && Double.compare(a.getPrice(), b.getPrice()) == 0;
    }
}
