package com.ardencm.settlements.instruction;

import com.ardencm.posttrade.dates.SettlementCycle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consumer-driven contract published by custody-gateway (contracts/settlement-instruction-service.json).
 * custody-gateway forwards our FIX to three custodians whose parsers require tag 64 as LocalMktDate.
 * Do not loosen this without a contract bump agreed with @ardencm/custody-integration.
 */
class CustodyGatewayContractTest {

    @Test
    @DisplayName("contract: tag 64 (SettlDate) is FIX LocalMktDate YYYYMMDD")
    void tag64IsLocalMktDate() throws Exception {
        JsonNode contract = load();
        Pattern expected = Pattern.compile(contract.at("/fix/tag64/pattern").asText());
        SettlementCycleConfig cfg = new SettlementCycleConfig();
        cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));
        InstructionBuilder b = new InstructionBuilder(cfg);
        for (JsonNode example : contract.at("/examples")) {
            Instruction i = b.build(example.get("tradeId").asText(), example.get("isin").asText(),
                    example.get("mic").asText(), LocalDate.parse(example.get("tradeDate").asText()));
            assertTrue(expected.matcher(i.fixSettlDate()).matches(),
                    "tag 64 must match " + expected + " but was " + i.fixSettlDate());
            assertEquals(example.get("expectedTag64").asText(), i.fixSettlDate());
        }
    }

    private static JsonNode load() throws Exception {
        try (InputStream in = CustodyGatewayContractTest.class.getResourceAsStream("/contracts/custody-gateway.json")) {
            assertNotNull(in, "contract file missing");
            return new ObjectMapper().readTree(in);
        }
    }
}
