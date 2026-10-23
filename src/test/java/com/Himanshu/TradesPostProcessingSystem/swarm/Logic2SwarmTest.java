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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** LOGIC-2: a trade with a NaN or infinite price must never be published to the booking topic. */
@Tag("swarm")
class Logic2SwarmTest {

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

    private void send(String price) throws Exception {
        mvc.perform(get("/api/trade/sendTrade").header("X-Account-Id", "ACCT-A")
                .param("symbol", "AAPL").param("quantity", "100").param("price", price));
    }

    @Test
    void nanPriceIsNotBooked() throws Exception {
        send("NaN");
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void infinitePriceIsNotBooked() throws Exception {
        send("Infinity");
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }
}
