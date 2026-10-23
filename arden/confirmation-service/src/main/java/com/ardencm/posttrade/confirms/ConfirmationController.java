package com.ardencm.posttrade.confirms;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ConfirmationController {
    private final ConfirmationService service;

    public ConfirmationController(ConfirmationService service) {
        this.service = service;
    }

    @GetMapping("/confirmations")
    public Confirmation confirm(@RequestParam String tradeId, @RequestParam String mic, @RequestParam LocalDate tradeDate) {
        return service.confirm(tradeId, mic, tradeDate);
    }
}
