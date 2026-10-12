# SUPPLY-4: vulnerable lz4-java 1.8.0 on the Kafka consumer decompression path (CVE-2025-12183, CVE-2025-66566)

| Field | Value |
|---|---|
| Component | `org.lz4:lz4-java` (transitive runtime dependency of `org.apache.kafka:kafka-clients` 3.8.1, via `spring-kafka` 3.3.2) |
| Resolved version | **1.8.0** (the `org.lz4` project is archived; maintained fork is `at.yawk.lz4:lz4-java`) |
| CVE / advisory | [CVE-2025-12183](https://nvd.nist.gov/vuln/detail/CVE-2025-12183) / [GHSA-vqf4-7m7x-wgfc](https://github.com/advisories/GHSA-vqf4-7m7x-wgfc): out-of-bounds memory operations on untrusted compressed input (DoS / adjacent memory read), fixed in `at.yawk.lz4:lz4-java` 1.8.1. [CVE-2025-66566](https://nvd.nist.gov/vuln/detail/CVE-2025-66566) / [GHSA-cmp6-m4wj-q63q](https://github.com/advisories/GHSA-cmp6-m4wj-q63q): Java safe decompressor leaks earlier output-buffer contents, fixed in `at.yawk.lz4:lz4-java` 1.10.1 |
| CISA KEV | **Not listed** (KEV catalog version 2026.10.08) |
| CWE | CWE-125 Out-of-bounds Read (CVE-2025-12183), CWE-226 Sensitive Information in Resource Not Removed Before Reuse (CVE-2025-66566) |
| Severity here | **Medium**: a producer with write access to `trades` (no broker auth configured) chooses the batch compression codec |
| Reachable | **Yes**: the consumer decodes LZ4 batches with the vulnerable jar (confirmed by test). The memory-corruption / leak primitive itself was **not** reproduced |

## Why it is reachable in this service
- Kafka consumers decompress whatever codec the producer used for the record batch. The service consumer config does not (and cannot)
  restrict codecs, so any LZ4 batch written to `trades` goes through `org.apache.kafka.common.compress.Lz4BlockInputStream`.
- `Lz4BlockInputStream` uses `LZ4Factory.fastestInstance().safeDecompressor()` and decompresses into a `decompressionBuffer` obtained from a
  reused `BufferSupplier`. A reused output buffer is the exact precondition for CVE-2025-66566 when the pure-Java implementation is
  selected (JNI unavailable, for example on musl or other unsupported platforms). CVE-2025-12183 covers the 1.8.0 implementations, JNI included.

- The other OSV entries against `org.lz4:lz4-java` 1.8.0 (CVE-2026-106449/-106450/-106452/-106453: `net.jpountz.lz4.LZ4BlockInputStream`,
  `LZ4FrameInputStream`, `LZ4DecompressorWithLength`; CVE-2026-59949: native XXHash; CVE-2026-106451: native library extraction to a shared temp dir)
  are **not** on the Kafka path. Kafka ships its own `org.apache.kafka.common.compress.Lz4BlockInputStream` and only calls the block-level
  `LZ4SafeDecompressor`. CVE-2026-106451 matters only on shared multi-user hosts.

## Confirmation test
`SupplySwarmTest#supply4_kafkaLz4BatchDecodeDoesNotUseVulnerableLz4Java` builds an LZ4 `MemoryRecords` batch with kafka-clients 3.8.1,
decodes it through the consumer record-batch iterator (1 record decoded), and asserts that the `net.jpountz.lz4` classes come from a fixed release:

```
[ERROR] SupplySwarmTest.supply4_kafkaLz4BatchDecodeDoesNotUseVulnerableLz4Java:204
[LZ4 decompressor used by the Kafka consumer must come from a fixed release (at.yawk.lz4 >= 1.10.1)]
Expecting actual:
  "file:/home/ubuntu/.m2/repository/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar"
not to contain:
  "org/lz4/lz4-java/1.8.0"
```

## Recommended owner action
- **Upgrade** to a Spring Boot / spring-kafka / kafka-clients line that ships `at.yawk.lz4:lz4-java` >= 1.10.1 (>= 1.11.4 to also clear the 2026 entries above). If none is available yet, the platform team
  should replace `org.lz4:lz4-java` with `at.yawk.lz4:lz4-java` >= 1.11.4 (same `net.jpountz.lz4` package) through managed dependency
  exclusion and replacement, after compatibility testing. The swarm does not make this change.
- **Interim mitigation:** turn on broker authentication and ACLs so that only the trade-capture producer can write to `trades`. Consider rejecting LZ4 at the
  broker with topic-level `compression.type` set to a non-LZ4 codec (the broker then recompresses).

## Reproduce
The confirmation test is not committed (report-only PRs contain only this advisory). Save the source below as
`src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm/SupplySwarmTest.java` on the swarm harness branch and run:

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B test -Pswarm -Dtest='SupplySwarmTest#supply4*'
```

<details><summary>SupplySwarmTest.java</summary>

```java
package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.service.TradeConsumerService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import jakarta.servlet.MultipartConfigElement;
import net.jpountz.lz4.LZ4Factory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.compress.Compression;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.messaging.Message;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SUPPLY persona: third-party components shipped by spring-boot-starter-parent 3.4.2.
 * Each test asserts the SAFE behavior. Vendor code is never patched here; see swarm/advisories/.
 */
@Tag("swarm")
class SupplySwarmTest {

    private WebServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop();
        }
    }

    @Configuration
    @EnableWebMvc
    static class WebConfig {
        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Trade> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        TradeService tradeService() {
            return new TradeService();
        }

        @Bean
        TradeController tradeController() {
            return new TradeController();
        }

        @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
        StandardServletMultipartResolver multipartResolver() {
            return new StandardServletMultipartResolver();
        }
    }

    /** Same embedded Tomcat, DispatcherServlet and multipart defaults Spring Boot 3.4.2 auto-configures for this app. */
    private int startAppTomcat() {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(WebConfig.class);
        MultipartConfigFactory mp = new MultipartConfigFactory();
        mp.setMaxFileSize(DataSize.ofMegabytes(1));
        mp.setMaxRequestSize(DataSize.ofMegabytes(10));
        MultipartConfigElement multipartConfig = mp.createMultipartConfig();
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        server = factory.getWebServer(servletContext -> {
            ctx.setServletContext(servletContext);
            var reg = servletContext.addServlet("dispatcherServlet", new DispatcherServlet(ctx));
            reg.addMapping("/");
            reg.setLoadOnStartup(1);
            reg.setMultipartConfig(multipartConfig);
        });
        server.start();
        return server.getPort();
    }

    // SUPPLY-1: CVE-2025-48988, tomcat-embed-core 10.1.34 (fixed 10.1.42 with maxPartCount=10, maxPartHeaderSize=512)
    @Test
    void supply1_tomcatRejectsMultipartPartFloodBeforeDispatch() throws Exception {
        int port = startAppTomcat();
        String boundary = "swarmBoundary";
        String bigHeader = "X-Pad: " + "A".repeat(4000) + "\r\n";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int parts = 1000;
        for (int i = 0; i < parts; i++) {
            body.writeBytes(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"p" + i + "\"\r\n"
                    + bigHeader + "\r\nx\r\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1));

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/trade/sendTrade"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());

        // 405 means Tomcat fully parsed all 1000 parts (~4 MB of part headers held in memory) and DispatcherServlet
        // only then discovered /sendTrade is GET-only. A patched Tomcat refuses the request during parsing.
        assertThat(resp.statusCode())
                .as("unauthenticated %d-part multipart body to a GET-only endpoint must be refused while parsing, not after", parts)
                .isNotEqualTo(405);
    }

    // SUPPLY-2: CVE-2025-24813 (CISA KEV), tomcat-embed-core 10.1.34. Needs a write-enabled DefaultServlet.
    @Test
    void supply2_partialPutToDefaultServletIsNotAccepted() throws Exception {
        int port = startAppTomcat();
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swarm.session"))
                        .header("Content-Range", "bytes 0-4/100")
                        .PUT(HttpRequest.BodyPublishers.ofString("rO0AB")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("partial PUT must not create a file").isNotIn(200, 201, 204);
        assertThat(new TomcatServletWebServerFactory().isRegisterDefaultServlet()).isFalse();
    }

    static class ExposedAdapter extends RecordMessagingMessageListenerAdapter<String, Trade> {
        ExposedAdapter(Object bean, Method method) {
            super(bean, method);
        }

        RecordMessageConverter converter() {
            return getMessageConverter();
        }
    }

    // SUPPLY-3: CVE-2026-41731, spring-kafka 3.3.2 (fixed 3.3.16). DefaultKafkaHeaderMapper trusts subpackages by prefix.
    @Test
    void supply3_listenerHeaderMapperDoesNotInstantiateTypesFromUntrustedSubpackages() throws Exception {
        Method listener = TradeConsumerService.class.getMethod("consumeTrade", Trade.class);
        ExposedAdapter adapter = new ExposedAdapter(new TradeConsumerService(), listener);

        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader("spring_json_header_types",
                "{\"x-evil\":\"java.util.concurrent.atomic.AtomicLong\"}".getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("x-evil", "42".getBytes(StandardCharsets.UTF_8)));
        ConsumerRecord<String, Trade> rec = new ConsumerRecord<>("trades", 0, 0L, -1L, TimestampType.CREATE_TIME,
                -1, -1, "k", new Trade("AAPL", 1, 1.0), headers, Optional.empty());

        Message<?> msg = adapter.converter().toMessage(rec, null, null, Trade.class);
        Object decoded = msg.getHeaders().get("x-evil");

        assertThat(decoded)
                .as("producer-chosen type java.util.concurrent.atomic.AtomicLong is outside the trusted java.util package"
                        + " and must not be instantiated by the @KafkaListener header mapper")
                .isNotInstanceOf(AtomicLong.class);
    }

    // SUPPLY-4: CVE-2025-12183 / CVE-2025-66566, org.lz4:lz4-java 1.8.0 pulled in by kafka-clients 3.8.1.
    @Test
    void supply4_kafkaLz4BatchDecodeDoesNotUseVulnerableLz4Java() {
        MemoryRecords records = MemoryRecords.withRecords(Compression.lz4().build(),
                new SimpleRecord("k".getBytes(StandardCharsets.UTF_8),
                        "{\"tradeId\":\"t\",\"symbol\":\"AAPL\",\"quantity\":1,\"price\":1.0}".getBytes(StandardCharsets.UTF_8)));
        int decoded = 0;
        for (RecordBatch batch : records.batches()) {
            for (Record r : batch) {
                decoded++;
            }
        }
        assertThat(decoded).as("consumer-side LZ4 batch decode path is exercised").isEqualTo(1);

        String lz4Jar = LZ4Factory.class.getProtectionDomain().getCodeSource().getLocation().toString();
        assertThat(lz4Jar)
                .as("LZ4 decompressor used by the Kafka consumer must come from a fixed release (at.yawk.lz4 >= 1.10.1)")
                .doesNotContain("org/lz4/lz4-java/1.8.0");
    }

    // SUPPLY-5: CVE-2026-89407, jackson-core 2.18.2 PATTERN_FLOAT ReDoS. Trade uses primitive double/int, whose
    // deserializers do not call NumberInput.looksLikeValidNumber(), so the Kafka payload path should stay linear.
    @Test
    void supply5_tradePayloadWithHugeNumericStringIsNotQuadratic() {
        JsonDeserializer<Trade> deser = new JsonDeserializer<>(Trade.class);
        deser.configure(Map.of(JsonDeserializer.TRUSTED_PACKAGES, "com.Himanshu.TradesPostProcessingSystem.model"), false);
        String evil = "1".repeat(60_000) + "x";
        byte[] payload = ("{\"tradeId\":\"t\",\"symbol\":\"AAPL\",\"quantity\":\"" + evil + "\",\"price\":\"" + evil + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        long t0 = System.nanoTime();
        try {
            deser.deserialize("trades", payload);
        } catch (Exception expected) {
            // rejection is fine; we only care that it is fast
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(ms).as("Trade deserialization of a 60k-char numeric string must not hit the quadratic regex").isLessThan(2000);
    }
}
```
</details>

This advisory is report-only. The vendor code has not been patched, forked, shaded or overridden.
