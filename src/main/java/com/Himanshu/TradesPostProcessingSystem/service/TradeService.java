package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TradeService {

    private static final String TOPIC = "trades";
    private static final Pattern SYMBOL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.\\-]{0,11}");

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String symbol, int quantity, double price) {
        // ✅ Trade Validation
        if (symbol == null || symbol.trim().isEmpty()) {
            return "❌ Error: Stock symbol cannot be empty!";
        }
        symbol = symbol.trim();
        if (!SYMBOL.matcher(symbol).matches()) {
            return "❌ Error: Stock symbol must be 1-12 characters of A-Z, 0-9, '.' or '-'!";
        }
        symbol = symbol.toUpperCase(Locale.ROOT);
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
