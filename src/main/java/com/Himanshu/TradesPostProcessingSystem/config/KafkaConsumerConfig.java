package com.Himanshu.TradesPostProcessingSystem.config;

import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private static final long RETRY_INTERVAL_MS = 1000L;
    private static final long MAX_RETRIES = 4L;

    @Bean
    public ConsumerFactory<String, Trade> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "trade-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.Himanshu.TradesPostProcessingSystem.model");

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new JsonDeserializer<>(Trade.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Trade> kafkaListenerContainerFactory(
            KafkaTemplate<String, Trade> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Trade> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Trades that still fail after retries are parked on "trades.DLT" for repair and replay instead of being skipped.
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES)));
        return factory;
    }
}
