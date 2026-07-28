package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.DispatcherType;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class CorrelatedExceptionBridgeFilterTest {

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final CorrelatedExceptionBridgeFilter filter =
            new CorrelatedExceptionBridgeFilter(resolver);

    @Test
    void marksObservationAndDelegatesPreControllerException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServerRequestObservationContext observation =
                new ServerRequestObservationContext(request, response);
        request.setAttribute(
                ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation);
        ClassCastException failure = new ClassCastException("cached collection type mismatch");
        when(resolver.resolveException(request, response, null, failure))
                .thenReturn(new ModelAndView());

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw failure;
        });

        assertThat(observation.getError()).isSameAs(failure);
        verify(resolver).resolveException(request, response, null, failure);
    }

    @Test
    void rethrowsWhenNoResolverCanHandleTheException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException failure = new IllegalStateException("unresolved");
        when(resolver.resolveException(request, response, null, failure)).thenReturn(null);

        assertThatThrownBy(() -> filter.doFilter(
                request, response, (ignoredRequest, ignoredResponse) -> {
                    throw failure;
                }))
                .isSameAs(failure);
    }

    @Test
    void leavesSuccessfulRequestsUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedApplication = new AtomicBoolean();

        filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> reachedApplication.set(true));

        assertThat(reachedApplication).isTrue();
        verify(resolver, never()).resolveException(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                isNull(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runsImmediatelyInsideTheSpringHttpObservationFilter() {
        FilterRegistration registration =
                CorrelatedExceptionBridgeFilter.class.getAnnotation(FilterRegistration.class);

        assertThat(registration).isNotNull();
        assertThat(registration.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
        assertThat(registration.dispatcherTypes()).containsExactly(DispatcherType.REQUEST);
        assertThat(registration.asyncSupported()).isTrue();
    }
}
