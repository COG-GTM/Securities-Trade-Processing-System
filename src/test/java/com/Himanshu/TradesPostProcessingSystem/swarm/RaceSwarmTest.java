package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.config.KafkaConsumerConfig;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
    void race4_tradeThatExhaustsRetriesIsParkedOnADeadLetterTopicNotDiscarded() throws Exception {
        KafkaTemplate<String, Trade> template = mock(KafkaTemplate.class);
        AtomicInteger deadLettered = new AtomicInteger();
        when(template.send(any(ProducerRecord.class))).thenAnswer(inv -> {
            deadLettered.incrementAndGet();
            return CompletableFuture.completedFuture(mock(SendResult.class));
        });

        KafkaConsumerConfig config = new KafkaConsumerConfig();
        Method factoryMethod = null;
        for (Method m : KafkaConsumerConfig.class.getMethods()) {
            if (m.getName().equals("kafkaListenerContainerFactory")) factoryMethod = m;
        }
        Object[] args = new Object[factoryMethod.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = KafkaTemplate.class.isAssignableFrom(factoryMethod.getParameterTypes()[i]) ? template : null;
        }
        ConcurrentKafkaListenerContainerFactory<String, Trade> factory =
                (ConcurrentKafkaListenerContainerFactory<String, Trade>) factoryMethod.invoke(config, args);
        ConcurrentMessageListenerContainer<String, Trade> container = factory.createContainer("trades");

        CommonErrorHandler handler = container.getCommonErrorHandler();
        assertNotNull(handler, "no error handler: spring-kafka falls back to DefaultErrorHandler(FixedBackOff(0, 9)), "
                + "which retries 10 times back-to-back and then logs and commits past the trade, so it is never booked");

        ConsumerRecord<String, Trade> record = new ConsumerRecord<>("trades", 0, 42L, "5b7d-db-outage",
                trade("5b7d-db-outage", "AAPL", 100, 150.50));
        MessageListenerContainer listenerContainer = mock(MessageListenerContainer.class);
        when(listenerContainer.isRunning()).thenReturn(true);
        Consumer<?, ?> consumer = mock(Consumer.class);
        RuntimeException dbDown = new org.springframework.dao.DataAccessResourceFailureException("Connection refused: tradesdb");
        for (int attempt = 0; attempt < 50 && deadLettered.get() == 0; attempt++) {
            try {
                handler.handleRemaining(dbDown, List.of(record), consumer, listenerContainer);
            } catch (RuntimeException rethrownForSeek) {
                // handler seeks back for redelivery until retries are exhausted
            }
        }

        verify(template, atLeastOnce()).send(argThat((ProducerRecord<String, Trade> r) ->
                r.topic().matches("trades[.-](DLT|dlt)") && "5b7d-db-outage".equals(r.key())));
    }
}
