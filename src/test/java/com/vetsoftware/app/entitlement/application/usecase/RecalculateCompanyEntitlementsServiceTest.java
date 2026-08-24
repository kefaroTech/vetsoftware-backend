package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.HOY;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.capacidadVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contrato;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contratoActivoConHistoria;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contratoEn;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.facturacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaTerminada;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.relojFijo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.out.AdminPermissionReconciliationPort;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.CompanyWithoutContractException;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ContractStatus;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecalculateCompanyEntitlementsService — reconstruir desde el contrato")
class RecalculateCompanyEntitlementsServiceTest {

    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private CompanyEntitlementRepository entitlementRepository;
    @Mock
    private CompanyCapacityRepository capacityRepository;
    @Mock
    private AdminPermissionReconciliationPort adminPermissionReconciliationPort;

    private RecalculateCompanyEntitlementsService service;

    @BeforeEach
    void setUp() {
        // El recalculador NO se mockea: no es un puerto, es la mecanica compartida por
        // los dos casos de uso que disparan el recalculo. Mockearlo dejaria el test
        // afirmando sobre nada.
        service = new RecalculateCompanyEntitlementsService(
                new CompanyEntitlementRecalculator(subscriptionQueryPort, entitlementRepository,
                        capacityRepository, adminPermissionReconciliationPort, relojFijo()));
    }

    private static RecalculateCompanyEntitlementsCommand comando() {
        return new RecalculateCompanyEntitlementsCommand(COMPANY_ID);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<CompanyEntitlement>> captorDePermisos() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<CompanyCapacity>> captorDeContadores() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Nested
    @DisplayName("Contrato vigente")
    class ContratoVigente {

        @Test
        @DisplayName("borra los permisos derivados de la empresa antes de reinsertarlos")
        void borra_antes_de_reinsertar() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(contratoActivoConHistoria()));

            service.execute(comando());

            InOrder orden = inOrder(entitlementRepository);
            orden.verify(entitlementRepository).deleteDerivedByCompanyId(COMPANY_ID);
            orden.verify(entitlementRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("reconcilia los permisos ADMIN después de persistir el cálculo efectivo")
        void reconcilia_admin_despues_de_persistir_el_calculo() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(contratoActivoConHistoria()));

            service.execute(comando());

            verify(adminPermissionReconciliationPort).reconcile(COMPANY_ID, AHORA);
        }

        @Test
        @DisplayName("guarda el modulo vigente en acceso completo y el dado de baja en solo lectura")
        void guarda_el_vigente_completo_y_el_de_baja_en_solo_lectura() {
            ContractSnapshot snapshot = contrato(contratoEn(ContractStatus.ACTIVE),
                    List.of(lineaVigente(900L, historiaClinica(), true),
                            lineaTerminada(800L, facturacion(), false, HOY.minusDays(2))),
                    List.of());
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(snapshot));

            EntitlementRecalculationDto resultado = service.execute(comando());

            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository).saveAll(guardados.capture());
            assertThat(guardados.getValue())
                    .extracting(permiso -> permiso.getSubModule().code(),
                            CompanyEntitlement::getAccessLevel)
                    .containsExactly(tuple("CLINICAL_HISTORY", AccessLevel.FULL),
                            tuple("BILLING", AccessLevel.NONE));
            assertThat(resultado.entitlementCount()).isEqualTo(2);
            assertThat(resultado.recalculatedAt()).isEqualTo(AHORA);
            assertThat(resultado.contractStatus()).isEqualTo("ACTIVE");
            assertThat(resultado.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("conserva el consumo del contador al recalcular su techo")
        void conserva_el_consumo_del_contador() {
            ContractSnapshot snapshot = contrato(contratoEn(ContractStatus.ACTIVE), List.of(),
                    List.of(capacidadVigente(910L, CapacityUnit.USER, 1, 2)));
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(snapshot));
            when(capacityRepository.findAllByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(contadorExistente(31L, CapacityUnit.USER, 10, 7)));

            service.execute(comando());

            ArgumentCaptor<List<CompanyCapacity>> guardados = captorDeContadores();
            verify(capacityRepository).saveAll(guardados.capture());
            assertThat(guardados.getValue()).singleElement().satisfies(contador -> {
                assertThat(contador.getId()).isEqualTo(31L);
                assertThat(contador.getLimitQuantity()).isEqualTo(3);
                assertThat(contador.getUsedQuantity()).isEqualTo(7);
                assertThat(contador.getRecalculatedAt()).isEqualTo(AHORA);
            });
        }
    }

    @Nested
    @DisplayName("Sin contrato vigente")
    class SinContratoVigente {

        @Test
        @DisplayName("cae al ultimo contrato y deja el acceso en solo lectura")
        void cae_al_ultimo_contrato_y_deja_solo_lectura() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.empty());
            when(subscriptionQueryPort.findLatestContractByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(contrato(contratoEn(ContractStatus.CANCELLED),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of())));

            service.execute(comando());

            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository).saveAll(guardados.capture());
            assertThat(guardados.getValue()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel)
                    .isEqualTo(AccessLevel.READ_ONLY);
        }

        @Test
        @DisplayName("sin ningun contrato falla y no toca la tabla")
        void sin_ningun_contrato_falla_y_no_toca_la_tabla() {
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
    }

    @Nested
    @DisplayName("Concesiones manuales")
    class ConcesionesManuales {

        @Test
        @DisplayName("el recalculo no borra ni reescribe una concesion a mano")
        void el_recalculo_no_toca_una_concesion_a_mano() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(contratoActivoConHistoria()));
            when(entitlementRepository.findManualGrantsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(concesionManual(facturacion())));

            EntitlementRecalculationDto resultado = service.execute(comando());

            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository).saveAll(guardados.capture());
            assertThat(guardados.getValue()).extracting(permiso -> permiso.getSubModule().code())
                    .containsExactly("CLINICAL_HISTORY").doesNotContain("BILLING");
            assertThat(resultado.manualGrantCount()).isEqualTo(1);
            assertThat(resultado.entitlementCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("el contrato no vuelve a derivar un submodulo que ya tiene concesion a mano")
        void el_contrato_no_pisa_el_submodulo_concedido_a_mano() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(
                            Optional.of(contrato(
                                    contratoEn(ContractStatus.ACTIVE), List.of(lineaTerminada(800L,
                                            historiaClinica(), true, HOY.minusDays(2))),
                                    List.of())));
            when(entitlementRepository.findManualGrantsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(concesionManual(historiaClinica())));

            service.execute(comando());

            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository).saveAll(guardados.capture());
            assertThat(guardados.getValue()).isEmpty();
        }

        @Test
        @DisplayName("recalcular dos veces sigue sin tocar la concesion a mano")
        void recalcular_dos_veces_sigue_sin_tocar_la_concesion() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(contratoActivoConHistoria()));
            when(entitlementRepository.findManualGrantsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(concesionManual(facturacion())));

            service.execute(comando());
            EntitlementRecalculationDto segunda = service.execute(comando());

            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository, times(2)).saveAll(guardados.capture());
            assertThat(guardados.getAllValues().get(1))
                    .extracting(permiso -> permiso.getSubModule().code())
                    .containsExactly("CLINICAL_HISTORY");
            assertThat(segunda.manualGrantCount()).isEqualTo(1);
        }

        private static CompanyEntitlement concesionManual(SubModuleRef subModule) {
            return new CompanyEntitlement(55L, COMPANY_ID, subModule, AccessLevel.FULL,
                    EntitlementSource.MANUAL_GRANT, null, null, AHORA.minusDays(20), null,
                    AHORA.minusDays(20), AHORA.minusDays(20));
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("recalcular dos veces guarda exactamente las mismas filas")
        void recalcular_dos_veces_guarda_lo_mismo() {
            when(subscriptionQueryPort.findCurrentContractByCompanyId(COMPANY_ID, HOY))
                    .thenReturn(Optional.of(contratoActivoConHistoria()));

            service.execute(comando());
            service.execute(comando());

            verify(entitlementRepository, times(2)).deleteDerivedByCompanyId(COMPANY_ID);
            ArgumentCaptor<List<CompanyEntitlement>> guardados = captorDePermisos();
            verify(entitlementRepository, times(2)).saveAll(guardados.capture());
            assertThat(huella(guardados.getAllValues().get(1)))
                    .isEqualTo(huella(guardados.getAllValues().getFirst()));
        }

        private static List<String> huella(List<CompanyEntitlement> permisos) {
            return permisos.stream()
                    .map(permiso -> permiso.getSubModule().id() + "|" + permiso.getAccessLevel()
                            + "|" + permiso.getSource() + "|" + permiso.getValidFrom() + "|"
                            + permiso.getValidUntil())
                    .toList();
        }
    }
}
