package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;

class GlobalExceptionHandlerTelemetryTest {

    @Test
    void recordsHandledServerErrorBeforeReturningItsCorrelatedProblemDetail() {
        String traceId = "21681c8c48fef44cf10671aec7948dfe";
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler(mock(AuditLogger.class), tracer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServerRequestObservationContext observation =
                new ServerRequestObservationContext(request, new MockHttpServletResponse());
        request.setAttribute(
                ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation);
        IllegalStateException failure = new IllegalStateException("unexpected");

        ProblemDetail problem = handler.handleUnexpected(failure, request);

        assertThat(observation.getError()).isSameAs(failure);
        assertThat(problem.getProperties()).containsEntry("traceId", traceId);
    }
}
