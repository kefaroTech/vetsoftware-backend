package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditFilterTest {

    private static final FilterChain PASS_THROUGH = (request, response) -> { };

    private final AuditLogger auditLogger = mock(AuditLogger.class);
    private final AuditFilter filter = new AuditFilter(auditLogger);

    @Test
    void auditsTheFourMutatingMethods() throws Exception {
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            filter.doFilter(new MockHttpServletRequest(method, "/api/v1/animals/9"),
                    new MockHttpServletResponse(), PASS_THROUGH);

            verify(auditLogger).mutation(eq(method), eq("/api/v1/animals/9"), eq(200),
                    eq("SUCCESS"), anyLong());
        }
    }

    @Test
    void ignoresReadOnlyMethods() throws Exception {
        for (String method : List.of("GET", "HEAD", "OPTIONS")) {
            filter.doFilter(new MockHttpServletRequest(method, "/api/v1/animals"),
                    new MockHttpServletResponse(), PASS_THROUGH);
        }

        verifyNoInteractions(auditLogger);
    }

    @Test
    void derivesTheOutcomeFromTheResponseStatus() throws Exception {
        audit("POST", "/api/v1/animals", 201);
        audit("POST", "/api/v1/animals", 400);
        audit("POST", "/api/v1/animals", 401);
        audit("POST", "/api/v1/animals", 403);
        audit("POST", "/api/v1/animals", 500);

        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(201), eq("SUCCESS"), anyLong());
        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(400), eq("REJECTED"), anyLong());
        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(401), eq("DENIED"), anyLong());
        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(403), eq("DENIED"), anyLong());
        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(500), eq("ERROR"), anyLong());
    }

    @Test
    void skipsInfrastructureEndpoints() throws Exception {
        for (String path : List.of("/actuator/health", "/swagger-ui/index.html", "/v3/api-docs")) {
            filter.doFilter(new MockHttpServletRequest("POST", path),
                    new MockHttpServletResponse(), PASS_THROUGH);
        }

        verifyNoInteractions(auditLogger);
    }

    @Test
    void auditsEvenWhenTheChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/animals");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            response.setStatus(500);
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        verify(auditLogger).mutation(eq("POST"), eq("/api/v1/animals"), eq(500), eq("ERROR"), anyLong());
    }

    private void audit(String method, String path, int status) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        filter.doFilter(new MockHttpServletRequest(method, path), response, PASS_THROUGH);
    }
}
