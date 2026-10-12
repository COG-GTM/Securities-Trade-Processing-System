package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class TradeService {

    private static final String TOPIC = "trades";
    private static final long SEND_TIMEOUT_SECONDS = 10;

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String symbol, int quantity, double price) {
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

        // ✅ Create Trade Object
        Trade trade = new Trade(symbol, quantity, price);

        // ✅ Send Trade to Kafka and only report success once the broker has acknowledged it
        try {
            kafkaTemplate.send(TOPIC, trade.getTradeId(), trade).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "❌ Error: Trade not confirmed by the broker, status unknown for tradeId " + trade.getTradeId();
        } catch (TimeoutException e) {
            return "❌ Error: Trade not confirmed by the broker, status unknown for tradeId " + trade.getTradeId();
        } catch (ExecutionException | RuntimeException e) {
            return "❌ Error: Trade was not accepted by the broker, please resubmit: tradeId " + trade.getTradeId();
        }
        return "✅ Trade sent successfully: " + trade;
    }
}
