package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** RACE persona: make one economic event settle twice or not at all. */
@Tag("swarm")
@SuppressWarnings({"unchecked", "rawtypes"})
class RaceSwarmTest {

    private static Trade trade(String tradeId, String symbol, int quantity, double price) {
        Trade t = new Trade();
        t.setTradeId(tradeId);
        t.setSymbol(symbol);
        t.setQuantity(quantity);
        t.setPrice(price);
        return t;
    }

    @Test
    void race3_brokerRejectionIsNotReportedAsSuccess() {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Trade>> rejected = new CompletableFuture<>();
        rejected.completeExceptionally(new KafkaException("Topic trades not present in metadata after 60000 ms."));
        when(template.send(anyString(), anyString(), any(Trade.class))).thenReturn(rejected);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);

        String response = service.generateAndSendTrade("AAPL", 100, 150.50);

        // If the caller is told "sent successfully" it will never resubmit, and the trade is never booked.
        assertFalse(response.contains("successfully"), "lost trade acknowledged as success: " + response);
        assertTrue(response.startsWith("❌"), "caller must get an error it can act on: " + response);
    }

    @Test
    void race3_acceptedSendIsStillReportedAsSuccess() {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), anyString(), any(Trade.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);

        assertTrue(service.generateAndSendTrade("AAPL", 100, 150.50).contains("Trade sent successfully"));
    }
}
