package com.ardencm.custody.gateway;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RoutingController {
    @PostMapping("/route")
    public Map<String, String> route(@RequestBody String fix) {
        String tag64 = null;
        for (String field : fix.split("\u0001")) {
            if (field.startsWith("64=")) tag64 = field.substring(3);
        }
        return Map.of("settlementDate", FixSettlDateParser.parse(tag64).toString(), "custodian", "EUROCLEAR");
    }
}
