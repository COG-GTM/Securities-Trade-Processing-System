package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AUTHZ-2: a caller with no desk identity must not be able to book a trade (it was booked on the HOUSE book). */
@Tag("swarm")
class Authz2SwarmTest {

    @Test
    @SuppressWarnings("unchecked")
    void anonymousCallerCannotBookTradeOnHouseBook() throws Exception {
        KafkaTemplate<String, Trade> kafka = mock(KafkaTemplate.class);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/trade/sendTrade")
                        .param("symbol", "AAPL").param("quantity", "1000000").param("price", "190.0"))
                .andExpect(status().is4xxClientError());

        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }
}
