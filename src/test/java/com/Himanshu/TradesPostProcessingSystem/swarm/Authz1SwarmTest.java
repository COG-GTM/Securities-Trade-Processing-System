package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeLifecycleController;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.repository.SettlementRepository;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.SettlementService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeAmendmentService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** AUTHZ-1: desk B must not read desk A's trade by guessing/enumerating its tradeId (IDOR). */
@Tag("swarm")
class Authz1SwarmTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        List<TradeEntity> store = new ArrayList<>();
        store.add(new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0));
        TradeRepository trades = mock(TradeRepository.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        when(trades.findByTradeId(anyString())).thenAnswer(i -> store.stream()
                .filter(t -> t.getTradeId().equals(i.getArgument(0))).findFirst().orElse(null));
        when(trades.findByTradeIdAndAccountId(anyString(), anyString())).thenAnswer(i -> store.stream()
                .filter(t -> t.getTradeId().equals(i.getArgument(0)) && t.getAccountId().equals(i.getArgument(1)))
                .findFirst().orElse(null));
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    @Test
    void otherDeskCannotReadForeignTrade() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-B"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("ACCT-A"))));
    }

    @Test
    void ownerStillReadsOwnTrade() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACCT-A"));
    }
}
