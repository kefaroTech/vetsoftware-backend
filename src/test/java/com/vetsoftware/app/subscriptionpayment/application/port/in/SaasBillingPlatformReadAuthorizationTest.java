package com.vetsoftware.app.subscriptionpayment.application.port.in;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.ListAllDunningEventsUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
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

@SpringJUnitConfig(SaasBillingPlatformReadAuthorizationTest.Cableado.class)
@DisplayName("Listados SaaS cross-tenant - exclusivos de plataforma")
class SaasBillingPlatformReadAuthorizationTest {

    @Autowired
    private ListAllSubscriptionPaymentsUseCase listPayments;
    @Autowired
    private ListAllDunningEventsUseCase listDunning;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN tenant no obtiene barridos globales aunque tenga authorities de lectura")
    void tenant_no_puede_listar_cross_tenant() {
        authenticate("ROLE_ADMIN", "subscriptionPayment.read", "dunningEvent.read");

        assertThatThrownBy(() -> listPayments.listAll(null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listDunning.listAll(null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SYSTEM puede listar y filtrar por cualquier empresa")
    void system_puede_listar_cross_tenant() {
        authenticate("ROLE_SYSTEM");

        assertThatCode(() -> listPayments.listAll(42L, 0, 20)).doesNotThrowAnyException();
        assertThatCode(() -> listDunning.listAll(42L, 0, 20)).doesNotThrowAnyException();
    }

    private static void authenticate(String... authorities) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("principal", "n/a", authorities));
    }

    static final class PaymentReadStub implements ListAllSubscriptionPaymentsUseCase {

        @Override
        public PageResult<SubscriptionPaymentDto> listAll(Long companyId, int page, int pageSize) {
            return PageResult.empty(page, pageSize);
        }
    }

    static final class DunningReadStub implements ListAllDunningEventsUseCase {

        @Override
        public PageResult<DunningEventDto> listAll(Long companyId, int page, int pageSize) {
            return PageResult.empty(page, pageSize);
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean
        PaymentReadStub paymentReadStub() {
            return new PaymentReadStub();
        }

        @Bean
        DunningReadStub dunningReadStub() {
            return new DunningReadStub();
        }
    }
}
