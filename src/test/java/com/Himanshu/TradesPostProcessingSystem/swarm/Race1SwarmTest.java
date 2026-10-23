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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RACE-1: a replayed or second settlement instruction must not settle the same trade twice. */
@Tag("swarm")
class Race1SwarmTest {

    private SettlementRepository settlements;
    private final List<SettlementEntity> stored = new ArrayList<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TradeRepository trades = mock(TradeRepository.class);
        settlements = mock(SettlementRepository.class);
        TradeEntity trade = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeId("T-100")).thenReturn(trade);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(trade);
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> {
            stored.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(settlements.findByInstructionId(anyString())).thenAnswer(i ->
                stored.stream().filter(s -> s.getInstructionId().equals(i.getArgument(0))).toList());
        when(settlements.findByTradeId(anyString())).thenAnswer(i ->
                stored.stream().filter(s -> s.getTradeId().equals(i.getArgument(0))).toList());
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    private org.springframework.test.web.servlet.ResultActions settle(String instructionId) throws Exception {
        return mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructionId\":\"" + instructionId + "\",\"tradeId\":\"T-100\"}"));
    }

    @Test
    void replayedInstructionSettlesOnlyOnce() throws Exception {
        settle("SI-1").andExpect(status().isOk());
        settle("SI-1").andExpect(status().isOk()).andExpect(jsonPath("$.cashAmount").value(19000.0));
        verify(settlements, times(1)).save(any(SettlementEntity.class));
    }

    @Test
    void settledTradeCannotBeSettledAgainUnderNewInstruction() throws Exception {
        settle("SI-1").andExpect(status().isOk());
        settle("SI-2").andExpect(status().isConflict());
        verify(settlements, times(1)).save(any(SettlementEntity.class));
    }
}
