package com.Himanshu.TradesPostProcessingSystem.controller;

import com.Himanshu.TradesPostProcessingSystem.model.TradeRequest;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    // POST + JSON only: browsers cannot send this cross-site without a CORS preflight.
    @PostMapping(value = "/sendTrade", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String sendTrade(@RequestBody TradeRequest request) {
        // ✅ Call Trade Service with Validation
        return tradeService.generateAndSendTrade(request.symbol(), request.quantity(), request.price());
    }
}
