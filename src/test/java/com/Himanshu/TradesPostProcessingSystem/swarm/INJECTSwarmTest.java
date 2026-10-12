package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.config.KafkaConsumerConfig;
import com.Himanshu.TradesPostProcessingSystem.controller.TradeController;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.Trade;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.TradeConsumerService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeService;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * INJECT persona: attacker-controlled input interpreted as code, markup, or log structure downstream.
 * Each inject<N>_ test asserts the SAFE behavior, so it fails while the exploit reproduces.
 */
@Tag("swarm")
class INJECTSwarmTest {

    private static final String UUID_RE = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private KafkaTemplate<String, Trade> kafkaTemplate;
    private TradeService tradeService;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        tradeService = new TradeService();
        ReflectionTestUtils.setField(tradeService, "kafkaTemplate", kafkaTemplate);
        TradeController controller = new TradeController();
        ReflectionTestUtils.setField(controller, "tradeService", tradeService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // INJECT-1: reflected script injection. The symbol is echoed back in the response; a browser that
    // asks for text/html must not get an HTML document that contains the attacker's <script>.
    @Test
    void inject1_scriptInSymbolIsNotReflectedAsHtml() throws Exception {
        MvcResult r = mvc.perform(get("/api/trade/sendTrade")
                        .param("symbol", "<script>alert(document.domain)</script>")
                        .param("quantity", "1")
                        .param("price", "1")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        String ct = String.valueOf(r.getResponse().getContentType());
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("INJECT-1 status=" + r.getResponse().getStatus() + " content-type=" + ct + " body=" + body);
        assertFalse(ct.contains("html") && body.contains("<script>"),
                "attacker <script> reflected in a " + ct + " response: " + body);
    }

    // INJECT-2: log forging. CR/LF in a trade field (from the REST symbol, or any producer on the topic)
    // must not create extra, attacker-authored lines in the consumer's log output.
    @Test
    void inject2_crlfInTradeFieldsCannotForgeConsumerLogLines() {
        TradeRepository repo = mock(TradeRepository.class);
        TradeConsumerService consumer = new TradeConsumerService();
        ReflectionTestUtils.setField(consumer, "tradeRepository", repo);

        Trade trade = new Trade();
        trade.setTradeId("6f1c2b1e-0000-4000-8000-000000000001");
        trade.setSymbol("AAPL'}\r\n✅ Trade stored in database: TradeEntity{id=999, tradeId='FORGED', symbol='MSFT', quantity=1000000, price=0.01}");
        trade.setQuantity(1);
        trade.setPrice(1.0);

        String out = captureStdout(() -> consumer.consumeTrade(trade));
        String[] lines = out.split("\\R");
        System.out.println("INJECT-2 consumer emitted " + lines.length + " lines:\n" + out);
        assertEquals(2, lines.length, "one consumeTrade call must emit exactly two log lines, got: " + Arrays.toString(lines));
        assertFalse(out.contains("tradeId='FORGED'") && Arrays.stream(lines).anyMatch(l -> l.startsWith("✅ Trade stored in database: TradeEntity{id=999")),
                "forged log line injected");
    }

    // INJECT-3: Kafka header injection. Any producer on the "trades" topic can set the __TypeId__ header;
    // the consumer must still materialise a Trade, not whatever type the header names.
    @Test
    void inject3_typeIdHeaderCannotRetypeTradePayload() {
        DefaultKafkaConsumerFactory<String, Trade> factory =
                (DefaultKafkaConsumerFactory<String, Trade>) new KafkaConsumerConfig().consumerFactory();
        Deserializer<Trade> deser = factory.getValueDeserializer();
        deser.configure(factory.getConfigurationProperties(), false);

        byte[] payload = "{\"tradeId\":\"t-1\",\"symbol\":\"AAPL\",\"quantity\":1,\"price\":1.0}".getBytes(StandardCharsets.UTF_8);
        RecordHeaders headers = new RecordHeaders();
        headers.add(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, "java.util.LinkedHashMap".getBytes(StandardCharsets.UTF_8));
        headers.add(AbstractJavaTypeMapper.DEFAULT_KEY_CLASSID_FIELD_NAME, "java.lang.String".getBytes(StandardCharsets.UTF_8));
        headers.add(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, "java.lang.Object".getBytes(StandardCharsets.UTF_8));

        Object value = ((Deserializer<?>) deser).deserialize("trades", headers, payload);
        System.out.println("INJECT-3 __TypeId__=java.util.LinkedHashMap -> deserialized as " + (value == null ? null : value.getClass().getName()));
        assertInstanceOf(Trade.class, value, "__TypeId__ header chose the payload type: " + (value == null ? null : value.getClass()));
    }

    // INJECT-4: second-order markup / control-character injection. The symbol is published to Kafka and
    // persisted for downstream settlement systems; markup and control characters must be rejected at capture.
    @Test
    void inject4_markupAndControlCharsInSymbolAreNotPublished() {
        String[] payloads = {
                "<img src=x onerror=alert(1)>",
                "AAPL\r\nINFO forged",
                "AAPL'; DROP TABLE trades;--",
                "=HYPERLINK(\"http://evil\")"
        };
        for (String p : payloads) {
            String resp = tradeService.generateAndSendTrade(p, 1, 1.0);
            System.out.println("INJECT-4 symbol=" + p.replace("\r", "\\r").replace("\n", "\\n") + " -> " + resp.replace("\r", "\\r").replace("\n", "\\n"));
        }
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Trade.class));
    }

    // INJECT-5: SQL/JPQL injection. Repository only uses derived, parameter-bound queries (expected to pass).
    @Test
    void inject5_repositoryUsesOnlyDerivedParameterBoundQueries() {
        for (Method m : TradeRepository.class.getDeclaredMethods()) {
            assertNull(m.getAnnotation(Query.class), m.getName() + " declares a hand-written @Query");
        }
    }

    // INJECT-6: Kafka key/header injection from the REST edge. Key is server-generated, no headers set (expected to pass).
    @Test
    @SuppressWarnings("unchecked")
    void inject6_kafkaKeyIsServerGeneratedNotAttackerControlled() {
        tradeService.generateAndSendTrade("AAPL", 1, 1.0);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("trades"), key.capture(), any(Trade.class));
        assertTrue(key.getValue().matches(UUID_RE), "Kafka key not a server UUID: " + key.getValue());
        verify(kafkaTemplate, never()).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    void validTradeIsStillAcceptedAndPublished() throws Exception {
        MvcResult r = mvc.perform(get("/api/trade/sendTrade")
                        .param("symbol", "BRK.B").param("quantity", "100").param("price", "150.50"))
                .andReturn();
        assertEquals(200, r.getResponse().getStatus());
        assertTrue(r.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("Trade sent successfully"));
        verify(kafkaTemplate).send(eq("trades"), anyString(), any(Trade.class));

        TradeRepository repo = mock(TradeRepository.class);
        TradeConsumerService consumer = new TradeConsumerService();
        ReflectionTestUtils.setField(consumer, "tradeRepository", repo);
        Trade t = new Trade("AAPL", 100, 150.5);
        String out = captureStdout(() -> consumer.consumeTrade(t));
        assertTrue(out.contains("symbol='AAPL'"));
        ArgumentCaptor<TradeEntity> saved = ArgumentCaptor.forClass(TradeEntity.class);
        verify(repo).save(saved.capture());
        assertEquals("AAPL", saved.getValue().getSymbol());
        assertEquals(t.getTradeId(), saved.getValue().getTradeId());
    }

    private static String captureStdout(Runnable r) {
        PrintStream orig = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            r.run();
        } finally {
            System.setOut(orig);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
