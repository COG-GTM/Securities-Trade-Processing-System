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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-2: CR/LF in a trade id path segment must not forge extra audit log lines. */
@Tag("swarm")
class Inject2SwarmTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(TradeQueryService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void crlfInTradeIdDoesNotForgeLogLines() throws Exception {
        appender.start();
        logger.addAppender(appender);
        TradeRepository trades = mock(TradeRepository.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TradeLifecycleController(
                new TradeQueryService(trades), new TradeAmendmentService(trades),
                new SettlementService(trades, mock(SettlementRepository.class)))).build();

        String forged = "T-404\r\n2026-10-23T13:00:00.000Z  INFO 1 --- [nio-8080-exec-1] c.H.T.service.SettlementService"
                + " : Settlement SI-9 for trade T-100 approved by account COMPLIANCE";
        mvc.perform(get("/api/trades/{tradeId}", forged).header("X-Account-Id", "ACCT-A"));

        assertTrue(!appender.list.isEmpty(), "trade lookup was not logged");
        for (ILoggingEvent e : appender.list) {
            String line = e.getFormattedMessage();
            assertFalse(line.contains("\n") || line.contains("\r"), "forged log line written: " + line);
        }
    }
}
