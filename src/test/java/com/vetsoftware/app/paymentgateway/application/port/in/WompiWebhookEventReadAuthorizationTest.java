package com.vetsoftware.app.paymentgateway.application.port.in;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(WompiWebhookEventReadAuthorizationTest.Cableado.class)
@DisplayName("Rastro de webhooks de Wompi - exclusivo de plataforma")
class WompiWebhookEventReadAuthorizationTest {

    @Autowired
    private ListWompiWebhookEventsUseCase listUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN tenant no obtiene el rastro aunque tenga authorities de lectura")
    void tenant_no_puede_listar() {
        authenticate("ROLE_ADMIN", "paymentGateway.read");

        assertThatThrownBy(() -> listUseCase
                .search(new ListWompiWebhookEventsQuery(null, null, null, null, 0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SYSTEM puede listar y filtrar por cualquier empresa")
    void system_puede_listar() {
        authenticate("ROLE_SYSTEM");

        assertThatCode(() -> listUseCase
                .search(new ListWompiWebhookEventsQuery(null, 42L, null, null, 0, 20)))
                .doesNotThrowAnyException();
    }

    private static void authenticate(String... authorities) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("principal", "n/a", authorities));
    }

    static final class ListStub implements ListWompiWebhookEventsUseCase {

        @Override
        public PageResult<WompiWebhookEventDto> search(ListWompiWebhookEventsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean
        ListStub listStub() {
            return new ListStub();
        }
    }
}
