package com.vetsoftware.app.company.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@SpringJUnitConfig(ProvisionCompanyAuthorizationTest.Cableado.class)
@DisplayName("ProvisionCompanyUseCase — alta de tenant exclusiva de plataforma")
class ProvisionCompanyAuthorizationTest {

    @Autowired
    private ProvisionCompanyUseCase useCase;
    @Autowired
    private CallCounter counter;

    @BeforeEach
    void resetCounter() {
        counter.reset();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("un EMPLOYEE con company.create no puede provisionar otro tenant")
    void employee_con_company_create_no_puede_provisionar_otro_tenant() {
        authenticate("ROLE_EMPLOYEE", "company.create");

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(counter.calls()).isZero();
    }

    @Test
    @DisplayName("ROLE_SYSTEM sí puede provisionar empresa y contrato inicial")
    void system_puede_provisionar_empresa_y_contrato() {
        authenticate("ROLE_SYSTEM");

        CompanyDto result = useCase.execute(command());

        assertThat(result.id()).isEqualTo(9L);
        assertThat(counter.calls()).isEqualTo(1);
    }

    private static void authenticate(String... authorities) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("principal", "n/a", authorities));
    }

    private static CreateCompanyCommand command() {
        return new CreateCompanyCommand("Clinica Norte", "NIT-900", "Calle 1", "3001234567", 11L);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean
        CallCounter callCounter() {
            return new CallCounter();
        }

        @Bean
        ProvisionCompanyUseCase provisionCompanyUseCase(CallCounter counter) {
            return command -> {
                counter.record();
                return new CompanyDto(9L, command.name(), command.identifier(), command.address(),
                        command.contactNumber(), new CitySummaryDto(command.cityId(), "Bogota"),
                        LocalDateTime.of(2026, 1, 15, 10, 30), true);
            };
        }
    }

    static final class CallCounter {
        private int calls;

        void record() {
            calls++;
        }

        int calls() {
            return calls;
        }

        void reset() {
            calls = 0;
        }
    }
}
