package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** LOGIC-2: new trades with a non-finite price or overflowing notional must never be published for booking. */
@Tag("swarm")
class Logic2SwarmTest {

    private KafkaTemplate<String, Trade> kafka;
    private TradeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafka = mock(KafkaTemplate.class);
        service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", kafka);
    }

    @ParameterizedTest
    @CsvSource({"100,NaN", "100,Infinity", "2147483647,1e300"})
    void nonFiniteTradeIsNotPublished(int quantity, double price) {
        assertThat(service.generateAndSendTrade("ACCT-A", "AAPL", quantity, price)).contains("Error");
        verify(kafka, never()).send(anyString(), anyString(), any(Trade.class));
    }
}
