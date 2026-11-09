package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private static final String TOPIC = "trades";

    @Autowired
    private KafkaTemplate<String, Trade> kafkaTemplate;

    public String generateAndSendTrade(String symbol, int quantity, double price) {
        // ✅ Trade Validation
        if (symbol == null || symbol.trim().isEmpty()) {
            log.warn("Rejected trade: empty symbol");
            return "❌ Error: Stock symbol cannot be empty!";
        }
        if (quantity <= 0) {
            log.warn("Rejected trade for {}: quantity {} must be greater than zero", symbol, quantity);
            return "❌ Error: Quantity must be greater than zero!";
        }
        if (price <= 0) {
            log.warn("Rejected trade for {}: price {} must be greater than zero", symbol, price);
            return "❌ Error: Price must be greater than zero!";
        }

        // ✅ Create Trade Object
        Trade trade = new Trade(symbol, quantity, price);

        // ✅ Send Trade to Kafka
        kafkaTemplate.send(TOPIC, trade.getTradeId(), trade);
        log.info("Published trade {} to topic {}", trade.getTradeId(), TOPIC);
        return "✅ Trade sent successfully: " + trade;
    }
}
