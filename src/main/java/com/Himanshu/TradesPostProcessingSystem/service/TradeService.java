package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TradeService {

    private static final String TOPIC = "trades";
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9./-]{0,15}");

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String symbol, int quantity, double price) {
        // ✅ Trade Validation
        if (symbol == null || symbol.trim().isEmpty()) {
            return "❌ Error: Stock symbol cannot be empty!";
        }
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            return "❌ Error: Stock symbol contains invalid characters!";
        }
        if (quantity <= 0) {
            return "❌ Error: Quantity must be greater than zero!";
        }
        if (price <= 0) {
            return "❌ Error: Price must be greater than zero!";
        }

        // ✅ Create Trade Object
        Trade trade = new Trade(symbol, quantity, price);

        // ✅ Send Trade to Kafka
        kafkaTemplate.send(TOPIC, trade.getTradeId(), trade);
        return "✅ Trade sent successfully: " + trade;
    }
}
