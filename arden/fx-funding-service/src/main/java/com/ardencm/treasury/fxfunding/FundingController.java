package com.ardencm.treasury.fxfunding;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class FundingController {
    private final FxFundingService service;

    public FundingController(FxFundingService service) {
        this.service = service;
    }

    @GetMapping("/funding-plan")
    public FundingPlan plan(@RequestParam String tradeId, @RequestParam String ccyPair,
                            @RequestParam String mic, @RequestParam LocalDate tradeDate) {
        return service.plan(tradeId, ccyPair, mic, tradeDate);
    }
}
