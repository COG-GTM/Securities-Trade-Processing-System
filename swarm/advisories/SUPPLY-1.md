# SUPPLY-1: Apache Tomcat multipart part-flood DoS reachable on every request path (CVE-2025-48988)

| Field | Value |
|---|---|
| Component | `org.apache.tomcat.embed:tomcat-embed-core` (embedded server, via `spring-boot-starter-web` → `spring-boot-starter-tomcat`) |
| Resolved version | **10.1.34**, managed by `spring-boot-starter-parent` **3.4.2** (`mvn dependency:list`) |
| CVE / advisory | [CVE-2025-48988](https://nvd.nist.gov/vuln/detail/CVE-2025-48988) / [GHSA-h3gc-qfqq-6h8f](https://github.com/advisories/GHSA-h3gc-qfqq-6h8f), affects 10.1.0-M1 to 10.1.41, fixed in **10.1.42** |
| Related | [CVE-2025-52520](https://nvd.nist.gov/vuln/detail/CVE-2025-52520) (multipart size-limit integer overflow, fixed 10.1.43), [CVE-2025-61795](https://nvd.nist.gov/vuln/detail/CVE-2025-61795) (multipart temp-file cleanup, fixed 10.1.47) |
| CISA KEV | **Not listed** (checked [KEV catalog](https://www.cisa.gov/known-exploited-vulnerabilities-catalog) version 2026.10.08) |
| CWE | CWE-770 Allocation of Resources Without Limits or Throttling |
| Severity here | **High**: unauthenticated, remote, single request, no special configuration |
| Reachable | **Yes** (confirmed by test) |

## Why it is reachable in this service
1. Spring Boot auto-configures multipart support by default (`spring.servlet.multipart.enabled=true`, 1 MB file / 10 MB request)
   and `StandardServletMultipartResolver`. The service never uses multipart, but it does not turn it off.
2. `DispatcherServlet.doDispatch()` calls `checkMultipart()` **before** handler lookup, so any request with
   `Content-Type: multipart/form-data` has every part parsed by Tomcat, on any path, before Spring finds out there is no handler for it.
3. Tomcat 10.1.34 has no `maxPartCount` / `maxPartHeaderSize` limits (added in 10.1.42 with defaults 10 and 512 bytes). The only cap is
   `maxParameterCount` (10,000), so one 10 MB request can carry thousands of parts with ~10 KB headers each, all of which are held in memory.
4. There is no authentication on `/api/trade/**` and no Spring Security, so this needs no credentials.

## Confirmation test
`SupplySwarmTest#supply1_tomcatRejectsMultipartPartFloodBeforeDispatch` (in
`src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm/SupplySwarmTest.java`) starts the same embedded Tomcat
(`TomcatServletWebServerFactory`) with a `DispatcherServlet` that serves only `TradeController` (`KafkaTemplate` is mocked) and the
Spring Boot 3.4.2 multipart defaults. It then POSTs one 1,000-part body (each part has a 4 KB header) to the GET-only `/api/trade/sendTrade`.
The test asserts the safe behavior: Tomcat refuses the body while parsing it. On 10.1.34 Tomcat parses all 1,000 parts and Spring only
then answers `405 Method Not Allowed`:

```
[ERROR] SupplySwarmTest.supply1_tomcatRejectsMultipartPartFloodBeforeDispatch:139
[unauthenticated 1000-part multipart body to a GET-only endpoint must be refused while parsing, not after]
Expecting actual:
  405
not to be equal to:
  405
```

## Recommended owner action (service owner / platform team)
- **Upgrade** to a Spring Boot release that manages `tomcat-embed-core` >= 10.1.42 (preferably the latest 10.1.x, which also fixes the
  related multipart CVEs and the 2026 Tomcat advisories listed by OSV against 10.1.34), or pin `tomcat.version` in `pom.xml`
  after compatibility testing. On Boot versions that expose them, set `server.tomcat.max-part-count` and `server.tomcat.max-part-header-size`.
- **Interim mitigation (no vendor change):** the service accepts no uploads, so set `spring.servlet.multipart.enabled=false`. The
  multipart resolver is then gone and Tomcat never parses part bodies for this app.
- After the upgrade, `mvn -B test -Pswarm -Dtest=SupplySwarmTest#supply1*` should pass.

## Reproduce
The confirmation test is not committed (report-only PRs contain only this advisory). Save the source below as
`src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm/SupplySwarmTest.java` on the swarm harness branch and run:

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B test -Pswarm -Dtest='SupplySwarmTest#supply1*'
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
