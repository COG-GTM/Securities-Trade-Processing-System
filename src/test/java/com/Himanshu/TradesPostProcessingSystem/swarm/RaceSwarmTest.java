package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private static KafkaTemplate<String, Trade> acceptingTemplate() {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), anyString(), any(Trade.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        return template;
    }

    private static MockMvc mvcOver(KafkaTemplate<String, Trade> template) {
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", service);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void race2_retriedSubmissionWithSameIdempotencyKeyBooksOneTrade() throws Exception {
        KafkaTemplate<String, Trade> template = acceptingTemplate();
        MockMvc mvc = mvcOver(template);

        // Client times out waiting for the 200 and retries; GET is also retried freely by proxies and browsers.
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-42")
                    .param("symbol", "AAPL").param("quantity", "100").param("price", "150.50"))
                    .andExpect(status().isOk());
        }
        verify(template, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void race2_concurrentDuplicateSubmissionsBookOneTrade() throws Exception {
        KafkaTemplate<String, Trade> template = acceptingTemplate();
        MockMvc mvc = mvcOver(template);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Throwable> errors = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-43")
                            .param("symbol", "MSFT").param("quantity", "10").param("price", "410.25"));
                } catch (Throwable t) {
                    synchronized (errors) { errors.add(t); }
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), () -> "requests failed: " + errors);
        verify(template, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void guard_distinctKeysAndKeylessRequestsStillBookSeparately() throws Exception {
        KafkaTemplate<String, Trade> template = acceptingTemplate();
        MockMvc mvc = mvcOver(template);
        mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-44")
                .param("symbol", "AAPL").param("quantity", "1").param("price", "1.0")).andExpect(status().isOk());
        mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-45")
                .param("symbol", "AAPL").param("quantity", "1").param("price", "1.0")).andExpect(status().isOk());
        mvc.perform(get("/api/trade/sendTrade")
                .param("symbol", "AAPL").param("quantity", "1").param("price", "1.0")).andExpect(status().isOk());
        verify(template, times(3)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void race2_idempotencyKeyReusedForADifferentTradeIsRejected() throws Exception {
        KafkaTemplate<String, Trade> template = acceptingTemplate();
        MockMvc mvc = mvcOver(template);
        mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-46")
                .param("symbol", "AAPL").param("quantity", "100").param("price", "150.50")).andExpect(status().isOk());
        String second = mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-46")
                .param("symbol", "AAPL").param("quantity", "100000").param("price", "150.50"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(second.contains("Error"), "a replayed key with altered economics must be rejected: " + second);
        verify(template, times(1)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void guard_retryAfterAFailedPublishIsPublishedAgain() throws Exception {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Trade>> brokerDown = new CompletableFuture<>();
        brokerDown.completeExceptionally(new org.springframework.kafka.KafkaException("broker unavailable"));
        when(template.send(anyString(), anyString(), any(Trade.class)))
                .thenReturn(brokerDown)
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        MockMvc mvc = mvcOver(template);

        // The first publish never reaches Kafka; an identical retry must publish rather than replay "already accepted".
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/trade/sendTrade").header("Idempotency-Key", "desk7-order-47")
                    .param("symbol", "AAPL").param("quantity", "100").param("price", "150.50"));
        }
        verify(template, times(2)).send(eq("trades"), anyString(), any(Trade.class));
    }

    @Test
    void guard_retryAfterASendThatThrowsIsPublishedAgain() {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), anyString(), any(Trade.class)))
                .thenThrow(new org.springframework.kafka.KafkaException("serializer unavailable"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        TradeService service = new TradeService();
        ReflectionTestUtils.setField(service, "kafkaTemplate", template);

        assertThrows(RuntimeException.class, () -> service.generateAndSendTrade("AAPL", 100, 150.50, "desk7-order-48"));
        assertTrue(service.generateAndSendTrade("AAPL", 100, 150.50, "desk7-order-48").contains("sent successfully"));
        verify(template, times(2)).send(eq("trades"), anyString(), any(Trade.class));
    }
}
