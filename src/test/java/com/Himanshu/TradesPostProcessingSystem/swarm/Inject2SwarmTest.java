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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** INJECT-2: CR/LF in the tradeId path segment forges extra lines in the trade-lookup audit log. */
@Tag("swarm")
class Inject2SwarmTest {

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
        String forged = "T-404\r\n2026-10-23T03:50:00.000Z  INFO 1 --- [nio-8080-exec-1] "
                + "c.H.T.service.TradeQueryService : Trade lookup T-100 by account ACCT-A";
        mvc.perform(get("/api/trades/{tradeId}", forged).header("X-Account-Id", "ACCT-B"));

        assertThat(appender.list).isNotEmpty();
        for (ILoggingEvent event : appender.list) {
            assertThat(event.getFormattedMessage())
                    .as("audit line carries attacker CR/LF: %s", event.getFormattedMessage())
                    .doesNotContain("\r").doesNotContain("\n");
        }
    }
}
