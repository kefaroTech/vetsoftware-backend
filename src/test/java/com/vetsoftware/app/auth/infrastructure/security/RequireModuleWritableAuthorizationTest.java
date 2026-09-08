package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.domain.SubModuleReadOnlyException;
import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.entitlement.application.port.in.FindCompanyAccessUseCase;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.ReactivateServiceUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Comprueba con {@code @EnableMethodSecurity} real —no llamando a
 * {@code Authz.requireModuleWritable} directamente, como hace
 * {@code AuthzTest}— que el {@code @PreAuthorize} de un puerto de escritura
 * bloquea de verdad cuando su submódulo está en {@code READ_ONLY}. Mismo
 * andamiaje que {@code CompanyAdministrationAuthorizationTest}.
 */
@SpringJUnitConfig(RequireModuleWritableAuthorizationTest.Cableado.class)
@DisplayName("@authz.requireModuleWritable — bloqueo real vía @PreAuthorize")
class RequireModuleWritableAuthorizationTest {

    private static final Long COMPANY_ID = 42L;
    private static final Long SERVICE_ID = 7L;

    // No es un bean a propósito: implementa un puerto con @PreAuthorize y,
    // registrado en el
    // contexto, @EnableMethodSecurity lo envolvería en un proxy JDK que ya no es de
    // esta clase.
    private static final MutableCompanyAccess COMPANY_ACCESS = new MutableCompanyAccess();

    @Autowired
    private ReactivateServiceUseCase reactivateService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SERVICES en READ_ONLY bloquea con SubModuleReadOnlyException, no con FORBIDDEN genérico")
    void services_read_only_bloquea_la_reactivacion() {
        authenticate("service.delete");
        COMPANY_ACCESS.setEntitlement("SERVICES", "READ_ONLY");

        assertThatThrownBy(() -> reactivateService.execute(SERVICE_ID, COMPANY_ID))
                .isInstanceOf(SubModuleReadOnlyException.class);
    }

    @Test
    @DisplayName("SERVICES en FULL deja reactivar")
    void services_full_deja_reactivar() {
        authenticate("service.delete");
        COMPANY_ACCESS.setEntitlement("SERVICES", "FULL");

        assertThatCode(() -> reactivateService.execute(SERVICE_ID, COMPANY_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("READ_ONLY de OTRO submódulo no bloquea SERVICES")
    void read_only_de_otro_submodulo_no_bloquea() {
        authenticate("service.delete");
        COMPANY_ACCESS.setEntitlement("CORE", "READ_ONLY");

        assertThatCode(() -> reactivateService.execute(SERVICE_ID, COMPANY_ID))
                .doesNotThrowAnyException();
    }

    private static void authenticate(String... authorities) {
        EmployeeContext context = new EmployeeContext(1L, COMPANY_ID, Set.of(authorities),
                Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(context, "n/a", authorities));
    }

    static final class ReactivateServiceStub implements ReactivateServiceUseCase {
        @Override
        public ServiceDto execute(Long id, Long companyId) {
            return null;
        }
    }

    /** El único punto donde cada test decide qué entitlement ve {@code Authz}. */
    static final class MutableCompanyAccess implements FindCompanyAccessUseCase {
        private volatile String subModuleCode = "SERVICES";
        private volatile String accessLevel = "FULL";

        void setEntitlement(String subModuleCode, String accessLevel) {
            this.subModuleCode = subModuleCode;
            this.accessLevel = accessLevel;
        }

        @Override
        public CompanyAccessDto findByCompanyId(Long companyId) {
            CompanyEntitlementDto entitlement = new CompanyEntitlementDto(1L, companyId,
                    new SubModuleSummaryDto(1L, subModuleCode, subModuleCode), accessLevel, "CORE",
                    null, null, LocalDateTime.now(), null, LocalDateTime.now());
            return new CompanyAccessDto(companyId, List.of(entitlement), List.of(),
                    LocalDateTime.now());
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean("authz")
        Authz authz() {
            return new Authz(COMPANY_ACCESS);
        }

        @Bean
        ReactivateServiceStub reactivateServiceStub() {
            return new ReactivateServiceStub();
        }
    }
}
