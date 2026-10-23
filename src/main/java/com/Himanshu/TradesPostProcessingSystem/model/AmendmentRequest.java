package com.Himanshu.TradesPostProcessingSystem.model;

/** Body of PUT /api/trades/{tradeId}/amend: the corrected quantity and price. */
public record AmendmentRequest(int quantity, double price) {
}
