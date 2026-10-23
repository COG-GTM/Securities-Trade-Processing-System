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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** RACE-1: replaying a settlement instruction (or sending a second one) must not move cash twice. */
@Tag("swarm")
class Race1SwarmTest {

    private final List<SettlementEntity> ledger = new CopyOnWriteArrayList<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TradeRepository trades = mock(TradeRepository.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        TradeEntity trade = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(trade);
        when(trades.findByTradeId("T-100")).thenReturn(trade);
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> {
            ledger.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(settlements.findByInstructionId(anyString())).thenAnswer(i -> ledger.stream()
                .filter(s -> s.getInstructionId().equals(i.getArgument(0))).toList());
        when(settlements.findByTradeId(anyString())).thenAnswer(i -> ledger.stream()
                .filter(s -> s.getTradeId().equals(i.getArgument(0))).toList());
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    private void settle(String instructionId) throws Exception {
        mvc.perform(post("/api/settlements").header("X-Account-Id", "ACCT-A")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructionId\":\"" + instructionId + "\",\"tradeId\":\"T-100\"}"));
    }

    @Test
    void replayedInstructionSettlesOnlyOnce() throws Exception {
        settle("SI-1");
        settle("SI-1");
        assertEquals(1, ledger.size(), "replayed SI-1 booked a second settlement / cash movement");
    }

    @Test
    void secondInstructionForSettledTradeIsRejected() throws Exception {
        settle("SI-1");
        settle("SI-2");
        assertEquals(1, ledger.size(), "already-settled trade T-100 settled again under SI-2");
        assertEquals(19000.0, ledger.stream().mapToDouble(SettlementEntity::getCashAmount).sum());
    }
}
