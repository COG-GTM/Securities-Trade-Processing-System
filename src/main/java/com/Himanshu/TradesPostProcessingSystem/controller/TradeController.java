package com.Himanshu.TradesPostProcessingSystem.controller;

import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/trade", produces = MediaType.TEXT_PLAIN_VALUE)
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @GetMapping("/sendTrade")
    public String sendTrade(
        @RequestHeader(value = "X-Account-Id", defaultValue = "HOUSE") String accountId,
        @RequestParam(value = "symbol") String symbol,
        @RequestParam(value = "quantity") int quantity,
        @RequestParam(value = "price") double price) {

        return tradeService.generateAndSendTrade(accountId, symbol, quantity, price);
    }
}
