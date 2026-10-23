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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AUTHZ-2: trades must not be booked over GET (CSRF) or with no caller identity (silent HOUSE default). */
@Tag("swarm")
class Authz2SwarmTest {

    private KafkaTemplate<String, Trade> kafka;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafka = mock(KafkaTemplate.class);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void anonymousCallerCannotBookTradeIntoHouseAccount() throws Exception {
        mvc.perform(post("/api/trade/sendTrade")
                        .param("symbol", "AAPL").param("quantity", "1000000").param("price", "190"))
                .andExpect(status().is4xxClientError());
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void anonymousGetCannotBookTrade() throws Exception {
        mvc.perform(get("/api/trade/sendTrade")
                        .param("symbol", "AAPL").param("quantity", "1000000").param("price", "190"))
                .andExpect(status().is4xxClientError());
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void tradeCannotBeBookedOverGet() throws Exception {
        mvc.perform(get("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "190"))
                .andExpect(status().isMethodNotAllowed());
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void identifiedDeskCanBookTradeOverPost() throws Exception {
        mvc.perform(post("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "190"))
                .andExpect(status().isOk());
        verify(kafka, times(1)).send(eq("trades"), anyString(), argThat(t -> "ACCT-A".equals(t.getAccountId())));
    }
}
