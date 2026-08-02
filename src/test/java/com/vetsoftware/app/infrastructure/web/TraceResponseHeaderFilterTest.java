package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceResponseHeaderFilterTest {

  @Test
  void exposesTheRealCurrentTraceWithoutGeneratingAnotherIdentifier() throws Exception {
    String traceId = "21681c8c48fef44cf10671aec7948dfe";
    Tracer tracer = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext context = mock(TraceContext.class);
    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(context);
    when(context.traceId()).thenReturn(traceId);
    TraceResponseHeaderFilter filter = new TraceResponseHeaderFilter(tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean reachedApplication = new AtomicBoolean();

    filter.doFilter(
        new MockHttpServletRequest(),
        response,
        (ignoredRequest, ignoredResponse) -> reachedApplication.set(true));

    assertThat(reachedApplication).isTrue();
    assertThat(response.getHeader(TraceResponseHeaderFilter.TRACE_HEADER)).isEqualTo(traceId);
    assertThat(response.getHeader(TraceResponseHeaderFilter.LEGACY_REQUEST_HEADER))
        .isEqualTo(traceId);
  }

  @Test
  void omitsTraceHeadersWhenThereIsNoCurrentSpan() throws Exception {
    Tracer tracer = mock(Tracer.class);
    TraceResponseHeaderFilter filter = new TraceResponseHeaderFilter(tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest(), response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getHeaderNames()).isEmpty();
  }
}
