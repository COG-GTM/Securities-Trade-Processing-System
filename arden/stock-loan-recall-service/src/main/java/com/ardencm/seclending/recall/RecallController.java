package com.ardencm.seclending.recall;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class RecallController {
    private final RecallService service;

    public RecallController(RecallService service) {
        this.service = service;
    }

    @GetMapping("/recalls")
    public RecallNotice recall(@RequestParam String loanId, @RequestParam String isin,
                               @RequestParam String mic, @RequestParam LocalDate saleTradeDate) {
        return service.recall(loanId, isin, mic, saleTradeDate);
    }
}
