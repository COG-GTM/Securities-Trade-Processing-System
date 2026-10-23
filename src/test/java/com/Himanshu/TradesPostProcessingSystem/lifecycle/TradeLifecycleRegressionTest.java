package com.Himanshu.TradesPostProcessingSystem.lifecycle;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Legitimate desk workflows that must keep working. Runs without Kafka or Postgres. */
@Tag("regression")
class TradeLifecycleRegressionTest {

    private TradeRepository trades;
    private SettlementRepository settlements;
    private MockMvc mvc;
    private TradeEntity owned;

    @BeforeEach
    void setUp() {
        trades = mock(TradeRepository.class);
        settlements = mock(SettlementRepository.class);
        owned = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeId("T-100")).thenReturn(owned);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(owned);
        when(trades.findByAccountId("ACCT-A")).thenReturn(List.of(owned));
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> i.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    @Test
    void ownerListsOwnTrades() throws Exception {
        mvc.perform(get("/api/trades").header("X-Account-Id", "ACCT-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tradeId").value("T-100"));
    }

    @Test
    void ownerReadsOwnTrade() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void unknownTradeIsNotFound() throws Exception {
        mvc.perform(get("/api/trades/T-404").header("X-Account-Id", "ACCT-A"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerAmendsQuantityAndPrice() throws Exception {
        mvc.perform(put("/api/trades/T-100/amend").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":150,\"price\":191.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(150))
                .andExpect(jsonPath("$.status").value("AMENDED"));
    }

    @Test
    void zeroQuantityAmendmentIsRejected() throws Exception {
        mvc.perform(put("/api/trades/T-100/amend").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0,\"price\":190.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerSettlesTradeOnce() throws Exception {
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instructionId\":\"SI-1\",\"tradeId\":\"T-100\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashAmount").value(19000.0));
        verify(settlements, times(1)).save(any(SettlementEntity.class));
    }

    @Test
    void otherAccountCannotSettle() throws Exception {
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-B")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instructionId\":\"SI-2\",\"tradeId\":\"T-100\"}"))
                .andExpect(status().isNotFound());
        verify(settlements, never()).save(any(SettlementEntity.class));
    }
}
