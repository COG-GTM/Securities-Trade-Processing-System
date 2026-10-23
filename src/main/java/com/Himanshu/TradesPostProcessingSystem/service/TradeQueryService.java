package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TradeQueryService {

    private static final Logger log = LoggerFactory.getLogger(TradeQueryService.class);

    private final TradeRepository tradeRepository;

    public TradeQueryService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public List<TradeEntity> listTrades(String accountId) {
        return tradeRepository.findByAccountId(accountId);
    }

    public TradeEntity getTrade(String accountId, String tradeId) {
        log.info("Trade lookup {} by account {}", forLog(tradeId), forLog(accountId));
        TradeEntity trade = tradeRepository.findByTradeIdAndAccountId(tradeId, accountId);
        if (trade == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade not found");
        }
        return trade;
    }

    private static String forLog(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
