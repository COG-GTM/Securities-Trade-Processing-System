package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeLifecycleController;
import com.Himanshu.TradesPostProcessingSystem.entity.SettlementEntity;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.repository.SettlementRepository;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.SettlementService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeAmendmentService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seed-time probe only (never committed to the seed branch). Each test asserts the safe behavior, so it FAILS while the flaw is present. */
@Tag("swarm")
class SeedProbeSwarmTest {

    private SettlementRepository settlements;
    private MockMvc mvc;
    private final List<SettlementEntity> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TradeRepository trades = mock(TradeRepository.class);
        settlements = mock(SettlementRepository.class);
        TradeEntity owned = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeId("T-100")).thenReturn(owned);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(owned);
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> { ledger.add(i.getArgument(0)); return i.getArgument(0); });
        when(settlements.findByInstructionId(anyString())).thenAnswer(i -> ledger.stream()
                .filter(s -> s.getInstructionId().equals(i.getArgument(0))).toList());
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(new TradeQueryService(trades),
                new TradeAmendmentService(trades), new SettlementService(trades, settlements))).build();
    }

    @Test
    void probeIdor_otherAccountCannotReadTrade() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-B")).andExpect(status().isNotFound());
    }

    @Test
    void probeNegativeAmend_negativeQuantityRejected() throws Exception {
        mvc.perform(put("/api/trades/T-100/amend").header("X-Account-Id", "ACCT-A")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-500,\"price\":190.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void probeDuplicateSettlement_sameInstructionSettlesOnce() throws Exception {
        String body = "{\"instructionId\":\"SI-9\",\"tradeId\":\"T-100\"}";
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A").contentType(MediaType.APPLICATION_JSON).content(body));
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A").contentType(MediaType.APPLICATION_JSON).content(body));
        assertEquals(1, ledger.size(), "settlement instruction SI-9 booked " + ledger.size() + " cash movements");
    }
}
