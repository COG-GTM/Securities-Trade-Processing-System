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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AUTHZ-2: booking a trade must need an explicit desk identity and must not be a GET (CSRF / link-click booking). */
@Tag("swarm")
class Authz2SwarmTest {

    private MockMvc mvc;
    private final List<Trade> published = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        KafkaTemplate<String, Trade> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), any(Trade.class))).thenAnswer(i -> {
            published.add(i.getArgument(2));
            return null;
        });
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void anonymousCallerCannotBookTradeOnHouseAccount() throws Exception {
        mvc.perform(post("/api/trade/sendTrade")
                .param("symbol", "AAPL").param("quantity", "1000000").param("price", "190.0"));
        mvc.perform(get("/api/trade/sendTrade")
                .param("symbol", "AAPL").param("quantity", "1000000").param("price", "190.0"));
        assertTrue(published.isEmpty(), "trade booked with no X-Account-Id: " + published);
    }

    @Test
    void getRequestCannotBookTrade() throws Exception {
        mvc.perform(get("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "190.0"))
                .andExpect(status().isMethodNotAllowed());
        assertTrue(published.isEmpty(), "state-changing GET booked a trade: " + published);
    }

    @Test
    void deskBooksTradeWithPostAndIdentity() throws Exception {
        mvc.perform(post("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "190.0"))
                .andExpect(status().isOk());
        assertEquals(1, published.size());
        assertEquals("ACCT-A", published.get(0).getAccountId());
    }
}
