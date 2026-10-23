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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** LOGIC-1: an amendment must not book a negative quantity or a non-finite price, which would settle negative/infinite cash. */
@Tag("swarm")
class Logic1SwarmTest {

    private TradeRepository trades;
    private MockMvc mvc;
    private TradeEntity owned;

    @BeforeEach
    void setUp() {
        trades = mock(TradeRepository.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        owned = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        when(trades.findByTradeIdAndAccountId("T-100", "ACCT-A")).thenReturn(owned);
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(i -> i.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades), new TradeAmendmentService(trades),
                new SettlementService(trades, settlements))).build();
    }

    @Test
    void negativeQuantityAmendmentIsRejected() throws Exception {
        mvc.perform(put("/api/trades/T-100/amend").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-100,\"price\":190.0}"))
                .andExpect(status().isBadRequest());
        assertEquals(100, owned.getQuantity());
        verify(trades, never()).save(any(TradeEntity.class));
    }

    @Test
    void infinitePriceAmendmentIsRejected() throws Exception {
        mvc.perform(put("/api/trades/T-100/amend").header("X-Account-Id", "ACCT-A")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":100,\"price\":1e400}"))
                .andExpect(status().isBadRequest());
        assertEquals(190.0, owned.getPrice());
        verify(trades, never()).save(any(TradeEntity.class));
    }
}
