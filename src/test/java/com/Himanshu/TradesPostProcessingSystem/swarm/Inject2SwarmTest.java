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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-2: CR/LF in the trade id path segment forges extra lines in the trade-lookup audit log. */
@Tag("swarm")
class Inject2SwarmTest {

    @Test
    void tradeLookupLogCannotBeForgedWithCrLf() throws Exception {
        TradeRepository trades = mock(TradeRepository.class);
        when(trades.findByTradeId("T-100")).thenReturn(new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades), new TradeAmendmentService(trades),
                new SettlementService(trades, mock(SettlementRepository.class)))).build();

        Logger logger = (Logger) LoggerFactory.getLogger(TradeQueryService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // %0D%0A in the path decodes to CR/LF in the @PathVariable
            mvc.perform(get(URI.create("/api/trades/T-404%0D%0A2026-10-23%20INFO%20SettlementService%20-%20Settlement%20SI-9%20SETTLED%20for%20trade%20T-100%20by%20account%20ACCT-A"))
                    .header("X-Account-Id", "ACCT-B"));
        } finally {
            logger.detachAppender(appender);
        }

        assertTrue(!appender.list.isEmpty(), "expected a trade lookup log line");
        for (ILoggingEvent e : appender.list) {
            String msg = e.getFormattedMessage();
            assertFalse(msg.contains("\n") || msg.contains("\r"), "forged log line injected: " + msg);
        }
    }
}
