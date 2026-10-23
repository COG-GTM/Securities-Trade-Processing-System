package com.ardencm.settlements.instruction;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class InstructionController {
    private final InstructionBuilder builder;

    public InstructionController(InstructionBuilder builder) {
        this.builder = builder;
    }

    @GetMapping("/instructions")
    public Map<String, String> build(@RequestParam String tradeId, @RequestParam String isin,
                                     @RequestParam String mic, @RequestParam LocalDate tradeDate) {
        Instruction i = builder.build(tradeId, isin, mic, tradeDate);
        return Map.of("fix", builder.toFix(i), "sese023", builder.toSese023(i),
                "settlementDate", i.settlementDate().toString());
    }
}
