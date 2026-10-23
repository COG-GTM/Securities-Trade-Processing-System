package com.Himanshu.TradesPostProcessingSystem.swarm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.Himanshu.TradesPostProcessingSystem.controller.TradeLifecycleController;
import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.repository.SettlementRepository;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.SettlementService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeAmendmentService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-2: a CR/LF in the tradeId path segment must not forge extra lines in the trade-lookup audit log. */
@Tag("swarm")
class Inject2SwarmTest {

    private static final String FORGED = "T-100\r\n2026-10-23T03:00:00Z INFO TradeQueryService - Trade lookup T-999 by account ACCT-COMPLIANCE";

    private final Logger logger = (Logger) LoggerFactory.getLogger(TradeQueryService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        TradeRepository trades = mock(TradeRepository.class);
        when(trades.findByTradeId("T-100")).thenReturn(new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0));
        mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades),
                new TradeAmendmentService(trades),
                new SettlementService(trades, mock(SettlementRepository.class)))).build();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void crlfInTradeIdCannotForgeAuditLines() throws Exception {
        mvc.perform(get("/api/trades/{tradeId}", FORGED).header("X-Account-Id", "ACCT-A"));

        assertFalse(appender.list.isEmpty(), "lookup was not audited");
        for (ILoggingEvent event : appender.list) {
            String line = event.getFormattedMessage();
            assertFalse(line.contains("\r") || line.contains("\n"), "forged log structure: " + line);
        }
    }

    @Test
    void normalLookupStillAudited() throws Exception {
        mvc.perform(get("/api/trades/T-100").header("X-Account-Id", "ACCT-A"));
        assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().equals("Trade lookup T-100 by account ACCT-A")));
    }
}
