package com.Himanshu.TradesPostProcessingSystem.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesIncomingCorrelationIdAndEchoesItOnResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trade/sendTrade");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(mdcDuringChain.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("abc-123");
    }

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        String echoed = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(echoed).isNotBlank();
        assertThat(UUID.fromString(echoed).toString()).isEqualTo(echoed);
        assertThat(mdcDuringChain.get()).isEqualTo(echoed);
    }

    @Test
    void generatesCorrelationIdWhenHeaderBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String echoed = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(UUID.fromString(echoed).toString()).isEqualTo(echoed);
    }

    @Test
    void replacesUnsafeOrOversizedHeaderValues() {
        assertThat(CorrelationIdFilter.resolveCorrelationId("bad\r\nvalue")).isNotEqualTo("bad\r\nvalue");
        assertThat(CorrelationIdFilter.resolveCorrelationId("x".repeat(129))).hasSize(36);
        assertThat(CorrelationIdFilter.resolveCorrelationId("  ok-id  ")).isEqualTo("ok-id");
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "req-1");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void restoresPreviousMdcValueAndClearsEvenWhenChainThrows() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "outer");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "inner");
        FilterChain failingChain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), failingChain))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("outer");
    }
}
