package com.ardencm.assetservicing.corpactions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class CorporateActionsController {
    private final ExDateCalculator calculator;

    public CorporateActionsController(ExDateCalculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/dividends/ex-date")
    public DividendEvent derive(@RequestParam String eventId, @RequestParam String isin, @RequestParam String primaryMic,
                                @RequestParam LocalDate recordDate, @RequestParam LocalDate paymentDate) {
        return calculator.derive(eventId, isin, primaryMic, recordDate, paymentDate);
    }
}
