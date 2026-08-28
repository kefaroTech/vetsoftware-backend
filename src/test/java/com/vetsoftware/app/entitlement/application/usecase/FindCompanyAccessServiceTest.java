package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.USUARIOS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.facturacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.relojFijo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCompanyAccessService — lo que la empresa puede usar ahora mismo")
class FindCompanyAccessServiceTest {

    @Mock
    private CompanyEntitlementRepository entitlementRepository;
    @Mock
    private CompanyCapacityRepository capacityRepository;

    private FindCompanyAccessService service;

    @BeforeEach
    void setUp() {
        service = new FindCompanyAccessService(entitlementRepository, capacityRepository,
                relojFijo());
    }

    private static CompanyEntitlement permiso(SubModuleRef subModule, AccessLevel level,
            LocalDateTime validUntil, LocalDateTime recalculatedAt) {
        return new CompanyEntitlement(1L, COMPANY_ID, subModule, level,
                EntitlementSource.SUBSCRIPTION, SUBSCRIPTION_ID, 900L, AHORA.minusDays(30),
                validUntil, recalculatedAt, AHORA.minusDays(30));
    }

    @Test
    @DisplayName("no devuelve la prueba ya caducada aunque su fila siga en la tabla")
    void no_devuelve_la_prueba_caducada() {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(permiso(historiaClinica(), AccessLevel.FULL, null, AHORA),
                        permiso(facturacion(), AccessLevel.FULL, AHORA.minusDays(1), AHORA)));

        CompanyAccessDto acceso = service.findByCompanyId(COMPANY_ID);

        assertThat(acceso.entitlements()).extracting(dto -> dto.subModule().code())
                .containsExactly("CLINICAL_HISTORY");
    }

    @Test
    @DisplayName("no devuelve el submodulo oculto: para la interfaz no existe")
    void no_devuelve_el_submodulo_oculto() {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(permiso(facturacion(), AccessLevel.NONE, null, AHORA)));

        assertThat(service.findByCompanyId(COMPANY_ID).entitlements()).isEmpty();
    }

    @Test
    @DisplayName("devuelve el de solo lectura: consultar e imprimir sigue estando permitido")
    void devuelve_el_de_solo_lectura() {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(
                List.of(permiso(historiaClinica(), AccessLevel.READ_ONLY, null, AHORA)));

        assertThat(service.findByCompanyId(COMPANY_ID).entitlements())
                .extracting(CompanyEntitlementDto::accessLevel).containsExactly("READ_ONLY");
    }

    @Test
    @DisplayName("informa el recalculo mas antiguo, que es el que delata un proceso caido")
    void informa_el_recalculo_mas_antiguo() {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(permiso(historiaClinica(), AccessLevel.FULL, null, AHORA),
                        permiso(facturacion(), AccessLevel.FULL, null, AHORA.minusDays(9))));

        assertThat(service.findByCompanyId(COMPANY_ID).recalculatedAt())
                .isEqualTo(AHORA.minusDays(9));
    }

    @Test
    @DisplayName("devuelve los contadores con su bandera de techo agotado")
    void devuelve_los_contadores_con_su_bandera() {
        when(entitlementRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(capacityRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(contadorExistente(31L, USUARIOS, 3, 5)));

        assertThat(service.findByCompanyId(COMPANY_ID).capacities()).singleElement()
                .satisfies(contador -> {
                    assertThat(contador.dimensionCode()).isEqualTo("USER");
                    assertThat(contador.measureKind()).isEqualTo("STOCK");
                    assertThat(contador.periodKey()).isEqualTo(PeriodKey.SENTINEL);
                    assertThat(contador.exhausted()).isTrue();
                });
    }
}
