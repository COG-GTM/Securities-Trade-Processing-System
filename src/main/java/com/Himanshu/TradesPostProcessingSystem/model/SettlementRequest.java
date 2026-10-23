package com.Himanshu.TradesPostProcessingSystem.model;

/** Body of POST /api/settlements: the custodian's settlement instruction for one trade. */
public record SettlementRequest(String instructionId, String tradeId) {
}
