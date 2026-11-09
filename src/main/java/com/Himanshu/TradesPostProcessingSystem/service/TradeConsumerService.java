package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeConsumerService {

    private static final Logger log = LoggerFactory.getLogger(TradeConsumerService.class);

    @Autowired
    private TradeRepository tradeRepository;

    @KafkaListener(topics = "trades", groupId = "trade-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeTrade(Trade trade) {
        log.info("Received trade: {}", trade);

        // Convert Trade model to TradeEntity (for database storage)
        TradeEntity tradeEntity = new TradeEntity(trade.getTradeId(), trade.getSymbol(), trade.getQuantity(), trade.getPrice());

        // Save trade to PostgreSQL
        tradeRepository.save(tradeEntity);

        log.info("Trade stored in database: {}", tradeEntity);
    }
}
