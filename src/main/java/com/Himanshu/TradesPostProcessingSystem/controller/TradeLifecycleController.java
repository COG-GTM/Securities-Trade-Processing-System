package com.Himanshu.TradesPostProcessingSystem.controller;

import com.Himanshu.TradesPostProcessingSystem.entity.SettlementEntity;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.AmendmentRequest;
import com.Himanshu.TradesPostProcessingSystem.model.SettlementRequest;
import com.Himanshu.TradesPostProcessingSystem.service.SettlementService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeAmendmentService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Post-trade lifecycle for booked trades. The calling desk identifies itself with the X-Account-Id header. */
@RestController
@RequestMapping("/api")
public class TradeLifecycleController {

    private final TradeQueryService queryService;
    private final TradeAmendmentService amendmentService;
    private final SettlementService settlementService;

    public TradeLifecycleController(TradeQueryService queryService, TradeAmendmentService amendmentService,
                                    SettlementService settlementService) {
        this.queryService = queryService;
        this.amendmentService = amendmentService;
        this.settlementService = settlementService;
    }

    @GetMapping("/trades")
    public List<TradeEntity> listTrades(@RequestHeader("X-Account-Id") String accountId) {
        return queryService.listTrades(accountId);
    }

    @GetMapping("/trades/{tradeId}")
    public TradeEntity getTrade(@RequestHeader("X-Account-Id") String accountId, @PathVariable String tradeId) {
        return queryService.getTrade(accountId, tradeId);
    }

    @PutMapping("/trades/{tradeId}/amend")
    public TradeEntity amendTrade(@RequestHeader("X-Account-Id") String accountId, @PathVariable String tradeId,
                                  @RequestBody AmendmentRequest request) {
        return amendmentService.amend(accountId, tradeId, request);
    }

    @PostMapping("/settlements")
    public SettlementEntity settle(@RequestHeader("X-Account-Id") String accountId, @RequestBody SettlementRequest request) {
        return settlementService.settle(accountId, request);
    }

    @GetMapping("/settlements/{instructionId}")
    public SettlementEntity getSettlement(@RequestHeader("X-Account-Id") String accountId, @PathVariable String instructionId) {
        return settlementService.getSettlement(accountId, instructionId);
    }
}
