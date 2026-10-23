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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** AUTHZ-1: one desk must not read another desk's trade by guessing its trade id (IDOR). */
@Tag("swarm")
class Authz1SwarmTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TradeRepository trades = mock(TradeRepository.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        TradeEntity deskA = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeId("T-100")).thenReturn(deskA);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(deskA);
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    @Test
    void otherDeskCannotReadTradeById() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-B"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("AAPL"))));
    }
}
