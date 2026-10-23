package com.Himanshu.TradesPostProcessingSystem.service;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.AmendmentRequest;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TradeAmendmentService {

    private final TradeRepository tradeRepository;

    public TradeAmendmentService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public TradeEntity amend(String accountId, String tradeId, AmendmentRequest request) {
        TradeEntity trade = tradeRepository.findByTradeIdAndAccountId(tradeId, accountId);
        if (trade == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade not found");
        }
        if ("SETTLED".equals(trade.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settled trades cannot be amended");
        }
        if (request.quantity() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amended quantity cannot be zero; cancel the trade instead");
        }
        if (!(request.price() > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than zero");
        }
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setStatus("AMENDED");
        return tradeRepository.save(trade);
    }
}
