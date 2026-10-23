package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RACE-2: booking a trade is exposed as a GET, a method that browsers, proxies, prefetchers and HTTP client
 * retry policies treat as safe to repeat. Each replay of the same URL books another trade.
 */
@Tag("swarm")
class Race2SwarmTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Trade> kafka = mock(KafkaTemplate.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void replayedGetDoesNotBookTrades() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                    .param("symbol", "AAPL").param("quantity", "100").param("price", "190.0"));
        }
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void postStillBooksOneTrade() throws Exception {
        mvc.perform(post("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "190.0"))
                .andExpect(status().isOk());
        verify(kafka, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }
}
