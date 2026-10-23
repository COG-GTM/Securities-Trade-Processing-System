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

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String accountId, String symbol, int quantity, double price) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return "❌ Error: Account cannot be empty!";
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            return "❌ Error: Stock symbol cannot be empty!";
        }
        if (quantity <= 0) {
            return "❌ Error: Quantity must be greater than zero!";
        }
        if (price <= 0) {
            return "❌ Error: Price must be greater than zero!";
        }

        Trade trade = new Trade(accountId, symbol, quantity, price);

        try {
            kafkaTemplate.send(TOPIC, trade.getTradeId(), trade).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "❌ Error: Trade not booked, send interrupted: " + trade.getTradeId();
        } catch (ExecutionException | TimeoutException e) {
            return "❌ Error: Trade not booked, broker did not acknowledge: " + trade.getTradeId();
        }
        return "✅ Trade sent successfully: " + trade;
    }
}
