package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.entity.SettlementEntity;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.SettlementRequest;
import com.Himanshu.TradesPostProcessingSystem.repository.SettlementRepository;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class SettlementService {

    private final TradeRepository tradeRepository;
    private final SettlementRepository settlementRepository;

    public SettlementService(TradeRepository tradeRepository, SettlementRepository settlementRepository) {
        this.tradeRepository = tradeRepository;
        this.settlementRepository = settlementRepository;
    }

    public SettlementEntity settle(String accountId, SettlementRequest request) {
        if (request.instructionId() == null || request.instructionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Settlement instruction id is required");
        }
        List<SettlementEntity> existing = settlementRepository.findByInstructionId(request.instructionId());
        if (!existing.isEmpty()) {
            SettlementEntity prior = existing.get(0);
            if (!prior.getAccountId().equals(accountId) || !prior.getTradeId().equals(request.tradeId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement instruction id already used");
            }
            return prior;
        }
        TradeEntity trade = tradeRepository.findByTradeIdAndAccountId(request.tradeId(), accountId);
        if (trade == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade not found");
        }
        SettlementEntity settlement = new SettlementEntity(request.instructionId(), trade.getTradeId(), accountId,
                trade.getQuantity(), trade.getQuantity() * trade.getPrice(), Instant.now());
        trade.setStatus("SETTLED");
        tradeRepository.save(trade);
        return settlementRepository.save(settlement);
    }

    public SettlementEntity getSettlement(String accountId, String instructionId) {
        return settlementRepository.findByInstructionId(instructionId).stream()
                .filter(s -> s.getAccountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement not found"));
    }
}
