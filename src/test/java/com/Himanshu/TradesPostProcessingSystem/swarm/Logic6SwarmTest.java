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
class Logic6SwarmTest {

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
    void logic6_malformedSymbolIsRejected() throws Exception {
        send("NOT A TICKER!!", "100", "150.50");
        send("A".repeat(50), "100", "150.50");
        send("\u0131", "100", "150.50"); // dotless i upper-cases to ASCII "I"
        send("stra\u00dfe", "100", "150.50"); // sharp s upper-cases to "SS"
        assertNothingPublished();
    }

    @Test
    void logic6_paddedSymbolIsBookedUnderCanonicalTicker() throws Exception {
        send("  aapl\t", "100", "150.50");
        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(kafkaTemplate).send(eq("trades"), anyString(), captor.capture());
        assertThat(captor.getValue().getSymbol()).isEqualTo("AAPL");
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
