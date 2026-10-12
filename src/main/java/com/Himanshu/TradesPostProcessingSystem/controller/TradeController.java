package com.Himanshu.TradesPostProcessingSystem.controller;

import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Value("${trade.api.key:}")
    private String apiKey;

    @GetMapping("/sendTrade")
    public ResponseEntity<String> sendTrade(
        @RequestHeader(value = "X-API-Key", required = false) String providedApiKey,
        @RequestParam(value = "symbol") String symbol,
        @RequestParam(value = "quantity") int quantity,
        @RequestParam(value = "price") double price) {

        if (!isAuthorized(providedApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Error: Unauthorized");
        }

        // ✅ Call Trade Service with Validation
        return ResponseEntity.ok(tradeService.generateAndSendTrade(symbol, quantity, price));
    }

    // Fails closed when no key is configured; constant-time compare.
    private boolean isAuthorized(String providedApiKey) {
        if (apiKey == null || apiKey.isBlank() || providedApiKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
            apiKey.getBytes(StandardCharsets.UTF_8),
            providedApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
