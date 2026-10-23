package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private static final String TOPIC = "trades";
    private static final java.util.regex.Pattern SYMBOL = java.util.regex.Pattern.compile("[A-Za-z0-9.\\-]{1,12}");

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String accountId, String symbol, int quantity, double price) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return "❌ Error: Account cannot be empty!";
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            return "❌ Error: Stock symbol cannot be empty!";
        }
        if (!SYMBOL.matcher(symbol).matches()) {
            return "❌ Error: Invalid stock symbol!";
        }
        if (quantity <= 0) {
            return "❌ Error: Quantity must be greater than zero!";
        }
        if (price <= 0) {
            return "❌ Error: Price must be greater than zero!";
        }

        Trade trade = new Trade(accountId, symbol, quantity, price);

        kafkaTemplate.send(TOPIC, trade.getTradeId(), trade);
        return "✅ Trade sent successfully: " + trade;
    }
}
