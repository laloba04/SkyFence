package com.skyfence.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private AtomicReference<String> mdcDuringRequest(MockHttpServletRequest request,
                                                     MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);
        return seen;
    }

    @Test
    void generatesIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = mdcDuringRequest(request, response);

        assertNotNull(seen.get(), "El MDC debe contener un requestId durante la petición");
        assertEquals(seen.get(), response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void respectsIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "cliente-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = mdcDuringRequest(request, response);

        assertEquals("cliente-123", seen.get());
        assertEquals("cliente-123", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void rejectsMaliciousHeaderAndGeneratesNewId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "inyec\ncion {maliciosa}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = mdcDuringRequest(request, response);

        assertNotEquals("inyec\ncion {maliciosa}", seen.get());
        assertNotNull(seen.get());
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        mdcDuringRequest(request, response);

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "El MDC debe limpiarse al terminar la petición");
    }
}
