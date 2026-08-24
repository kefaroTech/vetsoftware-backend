package com.vetsoftware.app.entitlement.domain;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.HOY;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.capacidadVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contrato;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contratoEn;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.enPruebaHasta;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.facturacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaTerminada;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaVigenteDeNucleo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("EntitlementCalculator — el contrato decide que se puede usar")
class EntitlementCalculatorTest {

    @Nested
    @DisplayName("Alta de modulo")
    class AltaDeModulo {

        @Test
        @DisplayName("una linea vigente concede acceso completo sin fecha de caducidad")
        void una_linea_vigente_concede_acceso_completo() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement().satisfies(permiso -> {
                assertThat(permiso.getAccessLevel()).isEqualTo(AccessLevel.FULL);
                assertThat(permiso.getSource()).isEqualTo(EntitlementSource.SUBSCRIPTION);
                assertThat(permiso.getValidUntil()).isNull();
                assertThat(permiso.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
                assertThat(permiso.getSubscriptionItemId()).isEqualTo(900L);
                assertThat(permiso.isActiveAt(AHORA)).isTrue();
            });
        }

        @Test
        @DisplayName("el modulo del nucleo se marca CORE y no SUBSCRIPTION")
        void el_modulo_del_nucleo_se_marca_core() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaVigenteDeNucleo(900L, historiaClinica())), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getSource).isEqualTo(EntitlementSource.CORE);
        }

        @Test
        @DisplayName("una linea que aun no ha empezado no concede nada")
        void una_linea_futura_no_concede_nada() {
            ModuleGrantLine futura = new ModuleGrantLine(900L, historiaClinica(), true,
                    HOY.plusDays(10), null, false);

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(futura), List.of()), AHORA);

            assertThat(resultado.entitlements()).isEmpty();
        }

        @Test
        @DisplayName("la linea vigente gana sobre una baja anterior del mismo submodulo")
        void la_linea_vigente_gana_sobre_una_baja_anterior() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaTerminada(800L, historiaClinica(), true, HOY.minusDays(3)),
                                    lineaVigente(900L, historiaClinica(), true)),
                            List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel).isEqualTo(AccessLevel.FULL);
        }
    }

    @Nested
    @DisplayName("Baja de modulo")
    class BajaDeModulo {

        @Test
        @DisplayName("dar de baja baja a solo lectura y deja la ventana abierta para siempre")
        void dar_de_baja_baja_a_solo_lectura() {
            LocalDate fin = HOY.minusDays(3);

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaTerminada(800L, historiaClinica(), true, fin)), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement().satisfies(permiso -> {
                assertThat(permiso.getAccessLevel()).isEqualTo(AccessLevel.READ_ONLY);
                assertThat(permiso.getValidFrom()).isEqualTo(fin.atStartOfDay());
                assertThat(permiso.getValidUntil()).isNull();
                assertThat(permiso.grantsAt(AHORA)).isTrue();
            });
        }

        @Test
        @DisplayName("el submodulo que no admite solo lectura queda oculto en vez de a medias")
        void el_submodulo_sin_solo_lectura_queda_oculto() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaTerminada(800L, facturacion(), false, HOY.minusDays(3))),
                            List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement().satisfies(permiso -> {
                assertThat(permiso.getAccessLevel()).isEqualTo(AccessLevel.NONE);
                assertThat(permiso.getAccessLevel().allowsRead()).isFalse();
            });
        }

        @Test
        @DisplayName("la baja mas reciente es la que explica el estado del submodulo")
        void la_baja_mas_reciente_explica_el_estado() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(
                            lineaTerminada(700L, historiaClinica(), true, HOY.minusMonths(3)),
                            lineaTerminada(800L, historiaClinica(), true, HOY.minusDays(2))),
                            List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement().satisfies(permiso -> {
                assertThat(permiso.getSubscriptionItemId()).isEqualTo(800L);
                assertThat(permiso.getValidFrom()).isEqualTo(HOY.minusDays(2).atStartOfDay());
            });
        }
    }

    @Nested
    @DisplayName("Caducidad de la prueba")
    class CaducidadDeLaPrueba {

        @Test
        @DisplayName("la prueba se marca TRIAL y caduca al arrancar el dia siguiente al ultimo")
        void la_prueba_caduca_al_dia_siguiente_del_ultimo() {
            LocalDate ultimoDia = HOY.plusDays(2);

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enPruebaHasta(ultimoDia),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            CompanyEntitlement permiso = resultado.entitlements().getFirst();
            assertThat(permiso.getSource()).isEqualTo(EntitlementSource.TRIAL);
            assertThat(permiso.getValidUntil()).isEqualTo(ultimoDia.plusDays(1).atStartOfDay());
            assertThat(permiso.isActiveAt(ultimoDia.atTime(23, 59))).isTrue();
            assertThat(permiso.isActiveAt(ultimoDia.plusDays(1).atStartOfDay())).isFalse();
        }

        @Test
        @DisplayName("la prueba caduca sola sin que nadie vuelva a recalcular")
        void la_prueba_caduca_sola_sin_recalcular() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enPruebaHasta(HOY),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            CompanyEntitlement permiso = resultado.entitlements().getFirst();
            assertThat(permiso.grantsAt(AHORA)).isTrue();
            assertThat(permiso.grantsAt(AHORA.plusDays(2))).isFalse();
        }
    }

    @Nested
    @DisplayName("Techo por estado del contrato")
    class TechoPorEstadoDelContrato {

        @Test
        @DisplayName("la mora no corta el acceso: PAST_DUE sigue siendo acceso completo")
        void la_mora_no_corta_el_acceso() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.PAST_DUE),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel).isEqualTo(AccessLevel.FULL);
        }

        @Test
        @DisplayName("el contrato en solo lectura degrada el modulo vigente")
        void el_contrato_en_solo_lectura_degrada_el_modulo() {
            EntitlementRecalculation resultado = EntitlementCalculator
                    .recalculate(COMPANY_ID,
                            contrato(contratoEn(ContractStatus.READ_ONLY),
                                    List.of(lineaVigente(900L, historiaClinica(), true),
                                            lineaVigente(901L, facturacion(), false)),
                                    List.of()),
                            AHORA);

            assertThat(resultado.entitlements())
                    .extracting(permiso -> permiso.getSubModule().code(),
                            CompanyEntitlement::getAccessLevel)
                    .containsExactly(tuple("CLINICAL_HISTORY", AccessLevel.READ_ONLY),
                            tuple("BILLING", AccessLevel.NONE));
        }

        @Test
        @DisplayName("un contrato cancelado conserva la consulta de lo que el cliente escribio")
        void un_contrato_cancelado_conserva_la_consulta() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.CANCELLED),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel)
                    .isEqualTo(AccessLevel.READ_ONLY);
        }

        @ParameterizedTest
        @EnumSource(ContractStatus.class)
        @DisplayName("ningun estado del contrato produce un corte total de acceso")
        void ningun_estado_produce_corte_total(ContractStatus status) {
            assertThat(status.maxAccessLevel()).isIn(AccessLevel.FULL, AccessLevel.READ_ONLY);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("recalcular dos veces el mismo contrato produce exactamente lo mismo")
        void recalcular_dos_veces_produce_lo_mismo() {
            ContractSnapshot contrato = contrato(contratoEn(ContractStatus.ACTIVE),
                    List.of(lineaVigente(900L, historiaClinica(), true),
                            lineaTerminada(800L, facturacion(), false, HOY.minusDays(4))),
                    List.of(capacidadVigente(910L, CapacityUnit.USER, 2, 3)));

            EntitlementRecalculation primera = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato, AHORA);
            EntitlementRecalculation segunda = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato, AHORA);

            assertThat(huella(segunda)).isEqualTo(huella(primera));
            assertThat(segunda.capacities())
                    .extracting(CompanyCapacity::getUnit, CompanyCapacity::getLimitQuantity)
                    .containsExactly(tuple(CapacityUnit.USER, 5));
        }

        private static List<String> huella(EntitlementRecalculation recalculo) {
            return recalculo.entitlements().stream()
                    .map(permiso -> permiso.getSubModule().id() + "|" + permiso.getAccessLevel()
                            + "|" + permiso.getSource() + "|" + permiso.getValidFrom() + "|"
                            + permiso.getValidUntil() + "|" + permiso.getSubscriptionItemId())
                    .toList();
        }
    }

    @Nested
    @DisplayName("Capacidades")
    class Capacidades {

        @Test
        @DisplayName("el techo suma lo incluido y lo comprado aparte, por unidad")
        void el_techo_suma_lo_incluido_y_lo_comprado() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(),
                            List.of(capacidadVigente(910L, CapacityUnit.USER, 2, 3),
                                    capacidadVigente(911L, CapacityUnit.USER, 0, 4),
                                    capacidadVigente(912L, CapacityUnit.BRANCH, 1, 1))),
                    AHORA);

            assertThat(resultado.capacities())
                    .extracting(CompanyCapacity::getUnit, CompanyCapacity::getLimitQuantity)
                    .containsExactly(tuple(CapacityUnit.USER, 9), tuple(CapacityUnit.BRANCH, 2));
        }

        @Test
        @DisplayName("una linea de capacidad terminada deja de sostener techo")
        void una_linea_terminada_deja_de_sostener_techo() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(),
                            List.of(new CapacityGrantLine(910L, CapacityUnit.USER, 3, 2,
                                    HOY.minusMonths(6), HOY.minusDays(1)))),
                    AHORA);

            assertThat(resultado.capacities()).isEmpty();
        }

        @Test
        @DisplayName("el recalculo nunca pisa lo que la empresa lleva usado")
        void el_recalculo_nunca_pisa_lo_usado() {
            List<CompanyCapacity> existentes = List.of(
                    contadorExistente(31L, CapacityUnit.USER, 10, 5),
                    contadorExistente(32L, CapacityUnit.BRANCH, 3, 2));
            List<CompanyCapacity> calculadas = List.of(CompanyCapacity.contracted(COMPANY_ID,
                    CapacityUnit.USER, 3, SUBSCRIPTION_ID, AHORA));

            List<CompanyCapacity> conciliadas = EntitlementCalculator.reconcile(existentes,
                    calculadas, AHORA);

            assertThat(conciliadas)
                    .extracting(CompanyCapacity::getUnit, CompanyCapacity::getId,
                            CompanyCapacity::getLimitQuantity, CompanyCapacity::getUsedQuantity)
                    .containsExactly(tuple(CapacityUnit.USER, 31L, 3, 5),
                            tuple(CapacityUnit.BRANCH, 32L, 0, 2));
        }

        @Test
        @DisplayName("bajar de plan deja al cliente por encima del techo, y eso es legitimo")
        void bajar_de_plan_deja_al_cliente_por_encima_del_techo() {
            List<CompanyCapacity> conciliadas = EntitlementCalculator.reconcile(
                    List.of(contadorExistente(31L, CapacityUnit.USER, 10, 5)),
                    List.of(CompanyCapacity.contracted(COMPANY_ID, CapacityUnit.USER, 3,
                            SUBSCRIPTION_ID, AHORA)),
                    AHORA);

            assertThat(conciliadas).singleElement().satisfies(contador -> {
                assertThat(contador.getUsedQuantity()).isGreaterThan(contador.getLimitQuantity());
                assertThat(contador.isExhausted()).isTrue();
            });
        }

        @Test
        @DisplayName("un contrato que ya no esta vigente no sostiene ningun techo")
        void un_contrato_no_vigente_no_sostiene_techo() {
            EntitlementRecalculation resultado = EntitlementCalculator
                    .recalculate(COMPANY_ID,
                            contrato(contratoEn(ContractStatus.EXPIRED), List.of(),
                                    List.of(capacidadVigente(910L, CapacityUnit.USER, 2, 3))),
                            AHORA);

            assertThat(resultado.capacities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Concesiones manuales")
    class ConcesionesManuales {

        private static CompanyEntitlement concesionManual(SubModuleRef subModule) {
            return new CompanyEntitlement(55L, COMPANY_ID, subModule, AccessLevel.FULL,
                    EntitlementSource.MANUAL_GRANT, null, null, AHORA.minusDays(20), null,
                    AHORA.minusDays(20), AHORA.minusDays(20));
        }

        @Test
        @DisplayName("el submodulo concedido a mano no se vuelve a derivar del contrato")
        void el_submodulo_concedido_a_mano_no_se_deriva() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA, List.of(concesionManual(historiaClinica())));

            assertThat(resultado.entitlements()).isEmpty();
        }

        @Test
        @DisplayName("la baja de contrato tampoco degrada lo concedido a mano")
        void la_baja_no_degrada_lo_concedido_a_mano() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaTerminada(800L, facturacion(), false, HOY.minusDays(3))),
                            List.of()),
                    AHORA, List.of(concesionManual(facturacion())));

            assertThat(resultado.entitlements()).isEmpty();
        }

        @Test
        @DisplayName("los demas submodulos se siguen derivando con normalidad")
        void los_demas_submodulos_se_siguen_derivando() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE),
                            List.of(lineaVigente(900L, historiaClinica(), true),
                                    lineaVigente(901L, facturacion(), false)),
                            List.of()),
                    AHORA, List.of(concesionManual(facturacion())));

            assertThat(resultado.entitlements())
                    .extracting(permiso -> permiso.getSubModule().code())
                    .containsExactly("CLINICAL_HISTORY");
        }
    }

    @Nested
    @DisplayName("Argumentos invalidos")
    class ArgumentosInvalidos {

        @Test
        @DisplayName("sin empresa no se calcula nada")
        void sin_empresa_no_se_calcula_nada() {
            assertThatThrownBy(() -> EntitlementCalculator.recalculate(null,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(), List.of()), AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id");
        }

        @Test
        @DisplayName("sin instante de recalculo no se calcula nada")
        void sin_instante_no_se_calcula_nada() {
            assertThatThrownBy(() -> EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(), List.of()), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("recalculation instant");
        }
    }
}
