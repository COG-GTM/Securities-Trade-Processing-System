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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** LOGIC-2: a trade already in SETTLED state must not settle again and book a second cash movement. */
@Tag("swarm")
class Logic2SwarmTest {

    private SettlementRepository settlements;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TradeRepository trades = mock(TradeRepository.class);
        settlements = mock(SettlementRepository.class);
        TradeEntity settled = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        settled.setStatus("SETTLED");
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(settled);
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> i.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    @Test
    void alreadySettledTradeCannotSettleAgain() throws Exception {
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instructionId\":\"SI-2\",\"tradeId\":\"T-100\"}"))
                .andExpect(status().isConflict());
        verify(settlements, never()).save(any(SettlementEntity.class));
    }
}
