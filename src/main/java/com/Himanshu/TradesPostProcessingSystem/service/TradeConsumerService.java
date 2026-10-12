package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeConsumerService {

    @Autowired
    private TradeRepository tradeRepository;

    @KafkaListener(topics = "trades", groupId = "trade-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeTrade(Trade trade) {
        System.out.println("✅ Received Trade: " + trade);

        // Kafka delivers at least once: a redelivered trade that is already booked is acknowledged without a second write.
        if (tradeRepository.findByTradeId(trade.getTradeId()) != null) {
            System.out.println("↩️ Duplicate delivery ignored, trade already stored: " + trade.getTradeId());
            return;
        }

        // Convert Trade model to TradeEntity (for database storage)
        TradeEntity tradeEntity = new TradeEntity(trade.getTradeId(), trade.getSymbol(), trade.getQuantity(), trade.getPrice());

        // Save trade to PostgreSQL; the unique trade_id constraint settles a race with a concurrent redelivery.
        try {
            tradeRepository.save(tradeEntity);
        } catch (DataIntegrityViolationException e) {
            if (tradeRepository.findByTradeId(trade.getTradeId()) == null) {
                throw e;
            }
            System.out.println("↩️ Duplicate delivery ignored, trade stored concurrently: " + trade.getTradeId());
            return;
        }

        System.out.println("✅ Trade stored in database: " + tradeEntity);
    }
}
