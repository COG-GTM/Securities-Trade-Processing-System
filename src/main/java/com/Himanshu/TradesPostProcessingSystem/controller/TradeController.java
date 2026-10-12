package com.Himanshu.TradesPostProcessingSystem.controller;

import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @GetMapping(value = "/sendTrade", produces = MediaType.TEXT_PLAIN_VALUE)
    public String sendTrade(
        @RequestParam(value = "symbol") String symbol,
        @RequestParam(value = "quantity") int quantity,
        @RequestParam(value = "price") double price) {
        
        // ✅ Call Trade Service with Validation
        return tradeService.generateAndSendTrade(symbol, quantity, price);
    }
}
