package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.TradeConsumerService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Tag("swarm")
class LogicSwarmTest {

    private KafkaTemplate<String, Trade> kafkaTemplate;
    private TradeRepository tradeRepository;
    private TradeConsumerService consumer;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafkaTemplate);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        tradeRepository = mock(TradeRepository.class);
        consumer = new TradeConsumerService();
        ReflectionTestUtils.setField(consumer, "tradeRepository", tradeRepository);
    }

    private void send(String symbol, String quantity, String price) throws Exception {
        mvc.perform(get("/api/trade/sendTrade")
                .param("symbol", symbol).param("quantity", quantity).param("price", price));
    }

    private void assertNothingPublished() {
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void logic4_consumerRejectsNegativeQuantityFromTopic() {
        Trade trade = new Trade("AAPL", -500, 150.50);
        consumer.consumeTrade(trade);
        verify(tradeRepository, never()).save(any(TradeEntity.class));
    }

    @Test
    void logic4_consumerRejectsZeroAndNanPriceFromTopic() {
        consumer.consumeTrade(new Trade("AAPL", 100, 0.0));
        consumer.consumeTrade(new Trade("AAPL", 100, Double.NaN));
        verify(tradeRepository, never()).save(any(TradeEntity.class));
    }

    @Test
    void logic4_consumerRejectsMissingSymbolOrTradeId() {
        consumer.consumeTrade(new Trade(null, 100, 150.50));
        Trade noId = new Trade("AAPL", 100, 150.50);
        noId.setTradeId(null);
        consumer.consumeTrade(noId);
        verify(tradeRepository, never()).save(any(TradeEntity.class));
    }

    @Test
    void validTradeIsStillPublished() throws Exception {
        send("AAPL", "100", "150.50");
        verify(kafkaTemplate, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void validTradeIsStillPersisted() {
        consumer.consumeTrade(new Trade("AAPL", 100, 150.50));
        verify(tradeRepository, times(1)).save(any(TradeEntity.class));
    }
}
