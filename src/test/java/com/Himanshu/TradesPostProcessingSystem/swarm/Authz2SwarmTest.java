package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("swarm")
class Authz2SwarmTest {

    private static final String API_KEY = "desk-a-test-key";

    private KafkaTemplate<String, Trade> kafkaTemplate;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafkaTemplate);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        if (ReflectionUtils.findField(TradeController.class, "apiKey") != null) {
            ReflectionTestUtils.setField(controller, "apiKey", API_KEY);
        }
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void authz2_crossSiteRequestCannotBookTrade() throws Exception {
        // <img src=".../sendTrade?..."> from a page the trader has open
        int crossSiteGet = mvc.perform(get("/api/trade/sendTrade").header("X-API-Key", API_KEY)
                        .header("Origin", "https://evil.example").header("Sec-Fetch-Site", "cross-site")
                        .param("symbol", "AAPL").param("quantity", "100").param("price", "150.5"))
                .andReturn().getResponse().getStatus();
        // auto-submitting <form method=post> (simple request, no CORS preflight)
        int crossSiteForm = mvc.perform(post("/api/trade/sendTrade").header("X-API-Key", API_KEY)
                        .header("Origin", "https://evil.example").header("Sec-Fetch-Site", "cross-site")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("symbol=AAPL&quantity=100&price=150.5"))
                .andReturn().getResponse().getStatus();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Trade.class));
        assertThat(crossSiteGet).isEqualTo(405);
        assertThat(crossSiteForm).isEqualTo(415);

        mvc.perform(post("/api/trade/sendTrade").header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\",\"quantity\":100,\"price\":150.5}"))
                .andExpect(status().isOk());
        verify(kafkaTemplate, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }
}
