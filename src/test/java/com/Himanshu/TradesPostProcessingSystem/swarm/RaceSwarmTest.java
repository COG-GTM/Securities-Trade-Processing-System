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

    @Test
    void race3_producerCannotBlockLongerThanTheSendBudget() {
        Object maxBlock = ((org.springframework.kafka.core.DefaultKafkaProducerFactory<String, Trade>)
                new com.Himanshu.TradesPostProcessingSystem.config.KafkaProducerConfig().producerFactory())
                .getConfigurationProperties().get(org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG);
        // Kafka's default max.block.ms is 60000: send() can hang on metadata long before the 10s get() budget starts.
        assertNotNull(maxBlock, "max.block.ms unset, so send() can block for 60s during a broker outage");
        assertTrue(Long.parseLong(maxBlock.toString()) <= 10_000L, "max.block.ms exceeds the send budget: " + maxBlock);
    }

    @Test
    void race3_ambiguousSendFailureDoesNotInviteABlindResubmission() {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Trade>> ackLost = new CompletableFuture<>();
        ackLost.completeExceptionally(new KafkaException("Expiring 1 record(s) for trades-0: 120000 ms has passed since batch creation"));
        when(template.send(anyString(), anyString(), any(Trade.class))).thenReturn(ackLost);
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);

        String response = service.generateAndSendTrade("AAPL", 100, 150.50);

        // A failed future does not prove the broker dropped the record; a resubmission gets a new tradeId and can book twice.
        assertFalse(response.contains("resubmit"), "caller told to resubmit an outcome that is unknown: " + response);
        assertTrue(response.contains("status unknown"), response);
    }
}
