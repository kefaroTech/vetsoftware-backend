package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaEntity;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code CompanyTaxProfileJpaEntity} se mockea porque su constructor sin
 * argumentos es {@code protected}, igual que {@code JpaBaseRoleQueryPortTest}
 * con {@code BaseRoleJpaEntity}.
 *
 * <p>
 * <strong>La entidad siempre se construye en una variable propia, nunca como
 * argumento inline de {@code when(...).thenReturn(...)}.</strong>
 * {@code perfilCon(...)} hace su propio {@code when/thenReturn} sobre la
 * entidad, y evaluarla como argumento de un {@code thenReturn} todavía abierto
 * dejaba a Mockito con un stubbing sin terminar
 * ({@code UnfinishedStubbingException}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyBillingEmailQueryPort")
class JpaCompanyBillingEmailQueryPortTest {

    private static final Long EMPRESA_A = 42L;
    private static final Long EMPRESA_B = 99L;

    @Mock
    private CompanyTaxProfileJpaRepository companyTaxProfileJpaRepository;
    @InjectMocks
    private JpaCompanyBillingEmailQueryPort port;

    private static CompanyTaxProfileJpaEntity perfilCon(String fiscalEmail) {
        CompanyTaxProfileJpaEntity entity = mock(CompanyTaxProfileJpaEntity.class);
        when(entity.getFiscalEmail()).thenReturn(fiscalEmail);
        return entity;
    }

    @Nested
    @DisplayName("busqueda del correo fiscal vigente")
    class BusquedaDelCorreoFiscalVigente {

        @Test
        @DisplayName("devuelve el correo fiscal de la ficha vigente de la empresa")
        void devuelve_el_correo_fiscal_de_la_ficha_vigente() {
            CompanyTaxProfileJpaEntity perfil = perfilCon("facturacion@clinica.co");
            when(companyTaxProfileJpaRepository.findCurrentByCompanyId(EMPRESA_A))
                    .thenReturn(Optional.of(perfil));

            assertThat(port.findFiscalEmail(EMPRESA_A)).contains("facturacion@clinica.co");
        }

        @Test
        @DisplayName("devuelve vacio si la empresa no tiene ficha fiscal vigente")
        void devuelve_vacio_si_no_hay_ficha_vigente() {
            when(companyTaxProfileJpaRepository.findCurrentByCompanyId(EMPRESA_A))
                    .thenReturn(Optional.empty());

            assertThat(port.findFiscalEmail(EMPRESA_A)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el correo fiscal de otra empresa no se devuelve")
        void el_correo_fiscal_de_otra_empresa_no_se_devuelve() {
            CompanyTaxProfileJpaEntity perfilEmpresaA = perfilCon("facturacion@clinica-a.co");
            when(companyTaxProfileJpaRepository.findCurrentByCompanyId(EMPRESA_A))
                    .thenReturn(Optional.of(perfilEmpresaA));
            when(companyTaxProfileJpaRepository.findCurrentByCompanyId(EMPRESA_B))
                    .thenReturn(Optional.empty());

            assertThat(port.findFiscalEmail(EMPRESA_A)).contains("facturacion@clinica-a.co");
            assertThat(port.findFiscalEmail(EMPRESA_B)).isEmpty();
        }
    }
}
