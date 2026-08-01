package com.vetsoftware.app.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.ObjectMapper;

class LoginRateLimitFilterTest {

    @SuppressWarnings("unchecked")
    private final LettuceBasedProxyManager<String> proxyManager = mock(LettuceBasedProxyManager.class);
    private final LoginRateLimitFilter filter = new LoginRateLimitFilter(
            proxyManager, new ObjectMapper(), mock(AuditLogger.class));

    @Test
    void filtersEverySensitivePublicPostRoute() {
        for (String path : List.of(
                "/auth/login/employee",
                "/auth/login/system",
                "/auth/refresh",
                "/register",
                "/auth/forgot-password",
                "/dian/webhooks/matias")) {
            assertThat(filter.shouldNotFilter(request("POST", path)))
                    .as("POST %s must be rate limited", path)
                    .isFalse();
        }
    }

    @Test
    void ignoresOtherRoutesAndNonPostRequests() {
        assertThat(filter.shouldNotFilter(request("GET", "/auth/login/employee"))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/logout"))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "/register/verify"))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "/dian/webhooks"))).isTrue();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
