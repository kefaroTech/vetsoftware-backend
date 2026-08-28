package com.vetsoftware.app.entitlement.domain;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.FIRMA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.HOY;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SEDES;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.USUARIOS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.capacidadVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contrato;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contratoEn;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.enPruebaHasta;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.facturacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaEnPrueba;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaInmuneALaDegradacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaTerminada;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaVigente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.lineaVigenteDeNucleo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("EntitlementCalculator — el contrato decide que se puede usar")
class EntitlementCalculatorTest {

    /**
     * El contador ya no se identifica por un enumerado sino por el eje del
     * catalogo, asi que las pruebas afirman sobre su codigo.
     */
    private static String codigoDeEje(CompanyCapacity capacity) {
        return capacity.getDimension().code();
    }

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
                    contrato(enPruebaHasta(ultimoDia), List.of(lineaEnPrueba(900L,
                            historiaClinica(), true, ultimoDia, TrialOutcomePolicy.LIMITED)),
                            List.of()),
                    AHORA);

            CompanyEntitlement permiso = resultado.entitlements().getFirst();
            assertThat(permiso.getSource()).isEqualTo(EntitlementSource.TRIAL);
            assertThat(permiso.getValidUntil()).isEqualTo(ultimoDia.plusDays(1).atStartOfDay());
            assertThat(permiso.isActiveAt(ultimoDia.atTime(23, 59))).isTrue();
            assertThat(permiso.isActiveAt(ultimoDia.plusDays(1).atStartOfDay())).isFalse();
        }

        /**
         * <b>R18 recorrida por el camino que la rompe, no preguntada a un
         * enumerado.</b> La regla dice que ningun camino del sistema deja a una empresa
         * sin ninguna fila que le conceda algo: el estado maximo de restriccion es
         * {@code READ_ONLY}, y no existe ningun valor que signifique «bloqueado».
         *
         * <p>
         * Al vencer la prueba, el recalculo emite un unico permiso con
         * {@code valid_until = ultimo dia + 1} y <strong>no emite ninguna fila
         * sucesora</strong>. Pasado ese instante la empresa no tiene nada que le
         * conceda: el acceso no baja, desaparece. Es literalmente el defecto que el
         * changeset 246 describe al abrir {@code FREE_LIMITED} y {@code EXPIRED_TRIAL}
         * —«sin esa segunda fila, al vencer la prueba el acceso no baja: DESAPARECE»— y
         * que el calculador todavia no cubre.
         *
         * <p>
         * <b>Se deja en rojo a proposito.</b> Adaptarlo al comportamiento de hoy seria
         * repetir el defecto que este encargo viene a quitar: un caso verde que no
         * prueba su regla.
         */
        @Test
        @DisplayName("R18 · al vencer la prueba la empresa conserva alguna fila que le concede: el"
                + " acceso baja a consulta, no desaparece")
        void al_vencer_la_prueba_queda_alguna_fila_que_concede() {
            LocalDate ultimoDia = HOY.plusDays(2);
            LocalDateTime yaVencida = ultimoDia.plusDays(1).atStartOfDay().plusMinutes(1);

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enPruebaHasta(ultimoDia),
                            List.of(lineaVigente(900L, historiaClinica(), true)), List.of()),
                    AHORA);

            assertThat(resultado.entitlements())
                    .as("filas que siguen concediendo algo una vez vencida la prueba")
                    .anySatisfy(permiso -> {
                        assertThat(permiso.grantsAt(yaVencida)).isTrue();
                        assertThat(permiso.getAccessLevel().allowsRead()).isTrue();
                    });
        }

        /**
         * <b>El bloqueante entero de la capa de prueba, recorrido por el camino que lo
         * rompe.</b>
         *
         * <p>
         * La version anterior de esta prueba afirmaba <em>lo contrario de la
         * regla</em>: comprobaba que dos dias despues el permiso ya no concedia, y
         * pasaba en verde sobre un defecto que dejaba a la clinica sin historia clinica
         * de un dia para otro. Un permiso que caduca sin nada detras no es "la prueba
         * caduca sola": es el acceso desapareciendo.
         *
         * <p>
         * Lo que el modelo decide (R-ENT-01) es que el acceso <strong>baja</strong>: la
         * fila sucesora se escribe el mismo dia que la de prueba, empezando donde la
         * otra acaba, y por eso la clave unica incluye {@code valid_from}. El caso
         * violador esta escrito como nombre de este metodo.
         */
        @Test
        @DisplayName("si el proceso diario no corre, Ana no se queda sin historia clinica:"
                + " pasa a modo limitado sola")
        void si_el_proceso_diario_no_corre_ana_no_se_queda_sin_historia_clinica_pasa_a_modo_limitado_sola() {
            LocalDate ultimoDia = HOY;
            LocalDateTime vencimiento = ultimoDia.plusDays(1).atStartOfDay();

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enPruebaHasta(ultimoDia), List.of(lineaEnPrueba(900L,
                            historiaClinica(), true, ultimoDia, TrialOutcomePolicy.LIMITED)),
                            List.of()),
                    AHORA);

            // Dos filas, escritas de una sola vez y sin solaparse.
            assertThat(resultado.entitlements()).hasSize(2)
                    .extracting(CompanyEntitlement::getSource, CompanyEntitlement::getValidFrom,
                            CompanyEntitlement::getValidUntil)
                    .containsExactly(
                            tuple(EntitlementSource.TRIAL, HOY.minusMonths(2).atStartOfDay(),
                                    vencimiento),
                            tuple(EntitlementSource.FREE_LIMITED, vencimiento, null));

            CompanyEntitlement prueba = resultado.entitlements().getFirst();
            CompanyEntitlement sucesora = resultado.entitlements().get(1);

            // Hoy manda la prueba y la sucesora todavia no ha empezado.
            assertThat(prueba.grantsAt(AHORA)).isTrue();
            assertThat(sucesora.isActiveAt(AHORA)).isFalse();

            // Dos dias despues la prueba ya no concede --eso no cambia-- pero el
            // acceso NO desaparece: lo recoge la sucesora, sin que nadie recalcule.
            assertThat(prueba.grantsAt(AHORA.plusDays(2))).isFalse();
            assertThat(sucesora.grantsAt(AHORA.plusDays(2))).isTrue();
            assertThat(sucesora.getAccessLevel()).isEqualTo(AccessLevel.FULL);
        }

        /**
         * R-ENT-06: no existe, ni debe implementarse, un estado de corte total. La
         * comprobacion se hace sobre el instante exacto del vencimiento, que es donde
         * vivia el agujero: ni un microsegundo sin fila que conceda.
         */
        @Test
        @DisplayName("ningun camino del sistema deja a una empresa sin ninguna fila"
                + " de entitlement")
        void ningun_camino_del_sistema_deja_a_una_empresa_sin_ninguna_fila_de_entitlement() {
            LocalDate ultimoDia = HOY;
            LocalDateTime vencimiento = ultimoDia.plusDays(1).atStartOfDay();

            for (TrialOutcomePolicy desenlace : TrialOutcomePolicy.values()) {
                EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                        contrato(enPruebaHasta(ultimoDia), List.of(
                                lineaEnPrueba(900L, historiaClinica(), true, ultimoDia, desenlace)),
                                List.of()),
                        AHORA);

                assertThat(resultado.entitlements())
                        .as("desenlace %s deja a la empresa sin acceso al vencer", desenlace)
                        .anyMatch(permiso -> permiso.grantsAt(vencimiento));
            }
        }

        /**
         * R-TRIAL-20: la traduccion entre los tres vocabularios esta escrita una sola
         * vez. Politica, modo de cobro y resultado son tres conceptos distintos y por
         * eso no comparten nombre; traducirlos en tres sitios es exactamente como un
         * desenlace acaba produciendo la terna de otro.
         */
        @Test
        @DisplayName("cada uno de los tres desenlaces produce exactamente su terna:"
                + " politica, modo y resultado no comparten nombre")
        void cada_uno_de_los_tres_desenlaces_produce_exactamente_su_terna() {
            LocalDate ultimoDia = HOY;

            assertThat(sucesoraDe(TrialOutcomePolicy.CONVERT_TO_PAID, ultimoDia))
                    .extracting(CompanyEntitlement::getSource, CompanyEntitlement::getAccessLevel)
                    .containsExactly(EntitlementSource.SUBSCRIPTION, AccessLevel.FULL);

            assertThat(sucesoraDe(TrialOutcomePolicy.LIMITED, ultimoDia))
                    .extracting(CompanyEntitlement::getSource, CompanyEntitlement::getAccessLevel)
                    .containsExactly(EntitlementSource.FREE_LIMITED, AccessLevel.FULL);

            assertThat(sucesoraDe(TrialOutcomePolicy.READ_ONLY, ultimoDia))
                    .extracting(CompanyEntitlement::getSource, CompanyEntitlement::getAccessLevel)
                    .containsExactly(EntitlementSource.EXPIRED_TRIAL, AccessLevel.READ_ONLY);
        }

        /**
         * R-TRIAL-15: el vencimiento barre por la fecha de cada linea, no por el estado
         * del contrato. Un dia de mora no puede matar la prueba para siempre.
         */
        @Test
        @DisplayName("Ana debe un dia la factura de DIAN, la paga, y sus tres modulos"
                + " en prueba siguen venciendo en su fecha")
        void ana_debe_un_dia_la_factura_de_dian_la_paga_y_sus_tres_modulos_en_prueba_siguen_venciendo_en_su_fecha() {
            LocalDate finPrueba = HOY.plusDays(5);
            SubscriptionRef enMora = new SubscriptionRef(SUBSCRIPTION_ID, ContractStatus.PAST_DUE,
                    null, FIRMA);

            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enMora, List.of(lineaEnPrueba(900L, historiaClinica(), true, finPrueba,
                            TrialOutcomePolicy.LIMITED)), List.of()),
                    AHORA);

            // El contrato esta en mora y la linea sigue siendo prueba, con SU fecha.
            assertThat(resultado.entitlements()).hasSize(2);
            assertThat(resultado.entitlements().getFirst().getSource())
                    .isEqualTo(EntitlementSource.TRIAL);
            assertThat(resultado.entitlements().getFirst().getValidUntil())
                    .isEqualTo(finPrueba.plusDays(1).atStartOfDay());
        }

        private static CompanyEntitlement sucesoraDe(TrialOutcomePolicy desenlace,
                LocalDate ultimoDia) {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(enPruebaHasta(ultimoDia), List
                            .of(lineaEnPrueba(900L, historiaClinica(), true, ultimoDia, desenlace)),
                            List.of()),
                    AHORA);
            return resultado.entitlements().get(1);
        }
    }

    @Nested
    @DisplayName("Inmunidad a la degradacion")
    class InmunidadALaDegradacion {

        /**
         * R-ENT-05: es la unica barandilla entre una discusion comercial y que una
         * clinica no pueda emitir sus facturas. Facturacion electronica no sabe
         * funcionar en solo lectura --emitir es escribir o nada--, asi que sin la marca
         * el techo de la mora la dejaria oculta del menu.
         */
        @Test
        @DisplayName("una cuenta en READ_ONLY por mora sigue pudiendo emitir facturas"
                + " electronicas")
        void una_cuenta_en_READ_ONLY_por_mora_sigue_pudiendo_emitir_facturas_electronicas() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.READ_ONLY),
                            List.of(lineaInmuneALaDegradacion(901L, facturacion())), List.of()),
                    AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel).isEqualTo(AccessLevel.FULL);
        }

        /** Sin la marca, el mismo submodulo desaparece del menu: NONE, no READ_ONLY. */
        @Test
        @DisplayName("sin la marca, el mismo submodulo se oculta al bajar el techo")
        void sin_la_marca_el_submodulo_se_oculta() {
            EntitlementRecalculation resultado = EntitlementCalculator
                    .recalculate(COMPANY_ID,
                            contrato(contratoEn(ContractStatus.READ_ONLY),
                                    List.of(lineaVigente(901L, facturacion(), false)), List.of()),
                            AHORA);

            assertThat(resultado.entitlements()).singleElement()
                    .extracting(CompanyEntitlement::getAccessLevel).isEqualTo(AccessLevel.NONE);
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

        /**
         * <b>Este caso se llamaba «ningun estado produce corte total» y no probaba
         * eso.</b> Interroga un enumerado: comprueba que
         * {@link ContractStatus#maxAccessLevel()} nunca devuelve algo por debajo de
         * {@code READ_ONLY}. Eso es cierto, es util y es solo el techo que impone el
         * estado del contrato — <em>una</em> de las entradas del calculo, no el
         * resultado. El nombre prometia la regla R18 entera y la regla R18 dice que
         * ningun camino del sistema deja a una empresa sin nada que le conceda.
         *
         * <p>
         * El caso de mas abajo, en {@code CaducidadDeLaPrueba}, recorre uno de esos
         * caminos de verdad.
         */
        @ParameterizedTest
        @EnumSource(ContractStatus.class)
        @DisplayName("el techo que impone el estado del contrato nunca baja de solo lectura")
        void el_techo_por_estado_del_contrato_nunca_baja_de_solo_lectura(ContractStatus status) {
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
                    List.of(capacidadVigente(910L, USUARIOS, 2, 3)));

            EntitlementRecalculation primera = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato, AHORA);
            EntitlementRecalculation segunda = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato, AHORA);

            assertThat(huella(segunda)).isEqualTo(huella(primera));
            assertThat(segunda.capacities()).extracting(EntitlementCalculatorTest::codigoDeEje,
                    CompanyCapacity::getLimitQuantity).containsExactly(tuple("USER", 5));
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
                            List.of(capacidadVigente(910L, USUARIOS, 2, 3),
                                    capacidadVigente(911L, USUARIOS, 0, 4),
                                    capacidadVigente(912L, SEDES, 1, 1))),
                    AHORA);

            assertThat(resultado.capacities())
                    .extracting(EntitlementCalculatorTest::codigoDeEje,
                            CompanyCapacity::getLimitQuantity)
                    .containsExactly(tuple("USER", 9), tuple("BRANCH", 2));
        }

        @Test
        @DisplayName("una linea de capacidad terminada deja de sostener techo")
        void una_linea_terminada_deja_de_sostener_techo() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.ACTIVE), List.of(),
                            List.of(new CapacityGrantLine(910L, USUARIOS, 3, 2, null,
                                    HOY.minusMonths(6), HOY.minusDays(1)))),
                    AHORA);

            assertThat(resultado.capacities()).isEmpty();
        }

        @Test
        @DisplayName("el recalculo nunca pisa lo que la empresa lleva usado")
        void el_recalculo_nunca_pisa_lo_usado() {
            List<CompanyCapacity> existentes = List.of(contadorExistente(31L, USUARIOS, 10, 5),
                    contadorExistente(32L, SEDES, 3, 2));
            List<CompanyCapacity> calculadas = List.of(CompanyCapacity.contracted(COMPANY_ID,
                    USUARIOS, PeriodKey.sentinel(), 3, SUBSCRIPTION_ID, AHORA));

            List<CompanyCapacity> conciliadas = EntitlementCalculator.reconcile(existentes,
                    calculadas, AHORA);

            assertThat(conciliadas)
                    .extracting(EntitlementCalculatorTest::codigoDeEje, CompanyCapacity::getId,
                            CompanyCapacity::getLimitQuantity, CompanyCapacity::getUsedQuantity)
                    .containsExactly(tuple("USER", 31L, 3, 5), tuple("BRANCH", 32L, 0, 2));
        }

        @Test
        @DisplayName("bajar de plan deja al cliente por encima del techo, y eso es legitimo")
        void bajar_de_plan_deja_al_cliente_por_encima_del_techo() {
            List<CompanyCapacity> conciliadas = EntitlementCalculator.reconcile(
                    List.of(contadorExistente(31L, USUARIOS, 10, 5)),
                    List.of(CompanyCapacity.contracted(COMPANY_ID, USUARIOS, PeriodKey.sentinel(),
                            3, SUBSCRIPTION_ID, AHORA)),
                    AHORA);

            assertThat(conciliadas).singleElement().satisfies(contador -> {
                assertThat(contador.getUsedQuantity()).isGreaterThan(contador.getLimitQuantity());
                assertThat(contador.isExhausted()).isTrue();
            });
        }

        @Test
        @DisplayName("un contrato que ya no esta vigente no sostiene ningun techo")
        void un_contrato_no_vigente_no_sostiene_techo() {
            EntitlementRecalculation resultado = EntitlementCalculator.recalculate(COMPANY_ID,
                    contrato(contratoEn(ContractStatus.EXPIRED), List.of(),
                            List.of(capacidadVigente(910L, USUARIOS, 2, 3))),
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
