package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.HOY;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contratoActivoConHistoria;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.relojFijo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.out.AdminPermissionReconciliationPort;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.application.port.out.EntitlementSnapshotPort;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.CompanyWithoutContractException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El caso de uso interno del alta. Comparte mecanica con el recalculo gateado,
 * y lo que se prueba aqui es que la comparte de verdad: mismas filas derivadas
 * y mismo fallo ruidoso cuando no hay contrato del que derivar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InitializeCompanyEntitlementsService — la primera derivacion del alta")
class InitializeCompanyEntitlementsServiceTest {

    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private CompanyEntitlementRepository entitlementRepository;
    @Mock
    private CompanyCapacityRepository capacityRepository;
    @Mock
    private AdminPermissionReconciliationPort adminPermissionReconciliationPort;
    @Mock
    private EntitlementSnapshotPort snapshotPort;

    private InitializeCompanyEntitlementsService service;

    @BeforeEach
    void setUp() {
        service = new InitializeCompanyEntitlementsService(new CompanyEntitlementRecalculator(
                subscriptionQueryPort, entitlementRepository, capacityRepository,
                adminPermissionReconciliationPort, snapshotPort, relojFijo()));
    }

    private static InitializeCompanyEntitlementsCommand comando() {
        return new InitializeCompanyEntitlementsCommand(COMPANY_ID);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<CompanyEntitlement>> captorDePermisos() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Test
    @DisplayName("deriva los permisos del contrato recien creado")
    void deriva_los_permisos_del_contrato_recien_creado() {
        when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                .thenReturn(Optional.of(contratoActivoConHistoria()));

        EntitlementRecalculationDto resultado = service.execute(comando());

        ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
        verify(entitlementRepository).saveAll(guardados.capture());
        assertThat(guardados.getValue()).singleElement().satisfies(permiso -> {
            assertThat(permiso.getSubModule().code()).isEqualTo("CLINICAL_HISTORY");
            assertThat(permiso.getAccessLevel()).isEqualTo(AccessLevel.FULL);
        });
        assertThat(resultado.recalculatedAt()).isEqualTo(AHORA);
        assertThat(resultado.manualGrantCount()).isZero();
        verify(adminPermissionReconciliationPort).reconcile(COMPANY_ID, AHORA);
    }

    @Test
    @DisplayName("una empresa que nace sin contrato no nace: falla y no escribe nada")
    void una_empresa_sin_contrato_no_nace() {
        when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                .thenReturn(Optional.empty());
        when(subscriptionQueryPort.findLatestContractByCompanyId(COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CompanyWithoutContractException.class)
                .hasMessageContaining("no subscription");

        verifyNoInteractions(entitlementRepository, capacityRepository,
                adminPermissionReconciliationPort);
    }

    @Test
    @DisplayName("sin empresa el comando ni se construye")
    void sin_empresa_el_comando_ni_se_construye() {
        assertThatThrownBy(() -> new InitializeCompanyEntitlementsCommand(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("company id");
    }
}
