package com.vetsoftware.app.company.application.port.in;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import java.util.Set;
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

@SpringJUnitConfig(CompanyAdministrationAuthorizationTest.Cableado.class)
@DisplayName("Administracion de empresas - fronteras SYSTEM y tenant")
class CompanyAdministrationAuthorizationTest {

    private static final Long COMPANY_ID = 42L;
    private static final Long OTHER_COMPANY_ID = 84L;

    @Autowired
    private CreateCompanyUseCase createCompany;
    @Autowired
    private DeleteCompanyUseCase deleteCompany;
    @Autowired
    private UpdateCompanyUseCase updateCompany;
    @Autowired
    private RecalculateCompanyEntitlementsUseCase recalculateEntitlements;
    @Autowired
    private ReactivateCompanyUseCase reactivateCompany;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("un tenant no crea, elimina ni recalcula empresas aunque conserve authorities")
    void tenant_no_puede_ejecutar_mutaciones_de_plataforma() {
        authenticateTenant("company.create", "company.delete", "entitlement.recalculate");

        assertThatThrownBy(() -> createCompany.execute(createCommand()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> deleteCompany.execute(COMPANY_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> recalculateEntitlements
                .execute(new RecalculateCompanyEntitlementsCommand(COMPANY_ID)))
                .isInstanceOf(AccessDeniedException.class);
        // Restaurar es el inverso de archivar y va al mismo regimen: si el tenant
        // pudiera hacerlo, tendria en la mano el «deshacer» de una decision de
        // plataforma sobre su propia suspension.
        assertThatThrownBy(() -> reactivateCompany.execute(COMPANY_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("company.update permite solo la empresa propia del tenant")
    void tenant_actualiza_solo_su_empresa() {
        authenticateTenant("company.update");

        assertThatCode(() -> updateCompany.execute(updateCommand(COMPANY_ID)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> updateCompany.execute(updateCommand(OTHER_COMPANY_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SYSTEM puede ejecutar todas las mutaciones administrativas")
    void system_puede_ejecutar_mutaciones_administrativas() {
        authenticateSystem();

        assertThatCode(() -> createCompany.execute(createCommand())).doesNotThrowAnyException();
        assertThatCode(() -> deleteCompany.execute(COMPANY_ID)).doesNotThrowAnyException();
        assertThatCode(() -> updateCompany.execute(updateCommand(COMPANY_ID)))
                .doesNotThrowAnyException();
        assertThatCode(() -> recalculateEntitlements
                .execute(new RecalculateCompanyEntitlementsCommand(COMPANY_ID)))
                .doesNotThrowAnyException();
        assertThatCode(() -> reactivateCompany.execute(COMPANY_ID)).doesNotThrowAnyException();
    }

    private static void authenticateTenant(String... authorities) {
        EmployeeContext context = new EmployeeContext(7L, COMPANY_ID, Set.of(authorities),
                Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(context, "n/a", authorities));
    }

    private static void authenticateSystem() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("system", "n/a", "ROLE_SYSTEM"));
    }

    private static CreateCompanyCommand createCommand() {
        return new CreateCompanyCommand("Clinica Norte", "NIT-900", "Calle 1", "3001234567", 11L);
    }

    private static UpdateCompanyCommand updateCommand(Long id) {
        return new UpdateCompanyCommand(id, "Clinica Norte", "NIT-900", "Calle 1", "3001234567",
                11L);
    }

    static final class CompanyAdministrationStub
            implements
                CreateCompanyUseCase,
                DeleteCompanyUseCase,
                UpdateCompanyUseCase,
                RecalculateCompanyEntitlementsUseCase {

        @Override
        public CompanyDto execute(CreateCompanyCommand command) {
            return null;
        }

        @Override
        public void execute(Long id) {
        }

        @Override
        public CompanyDto execute(UpdateCompanyCommand command) {
            return null;
        }

        @Override
        public EntitlementRecalculationDto execute(RecalculateCompanyEntitlementsCommand command) {
            return null;
        }
    }

    /**
     * En clase aparte y no dentro de {@link CompanyAdministrationStub}: los dos
     * puertos declaran {@code execute(Long)} y solo cambia el tipo de retorno
     * —{@code void} en el borrado, {@code CompanyDto} en la reactivacion—, y Java
     * no deja implementar dos firmas que solo difieren en eso.
     */
    static final class CompanyReactivationStub implements ReactivateCompanyUseCase {

        @Override
        public CompanyDto execute(Long id) {
            return null;
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean("authz")
        Authz authz() {
            return new Authz();
        }

        @Bean
        CompanyAdministrationStub companyAdministrationStub() {
            return new CompanyAdministrationStub();
        }

        @Bean
        CompanyReactivationStub companyReactivationStub() {
            return new CompanyReactivationStub();
        }
    }
}
