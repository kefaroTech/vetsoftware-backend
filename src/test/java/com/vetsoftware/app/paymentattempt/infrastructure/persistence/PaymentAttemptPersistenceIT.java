package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaPaymentAttemptRepository} contra MySQL real.
 *
 * <p>
 * <b>La regla que esta clase existe para vigilar es la exencion de los errores
 * propios.</b> Un rechazo {@link DeclineKind#CONFIGURATION} —credencial mal
 * puesta, pasarela caida, moneda no soportada— no gasta el presupuesto de
 * reintentos del cliente ni arranca cobranza contra el. Eso esta escrito en dos
 * sitios: en el dominio ({@code PaymentAttempt.consumesCustomerAttempts}) y en
 * el SQL del contador ({@code countChargeableSince}, que excluye la clase por
 * parametro). El dominio lo cubre un test de dominio; <b>el filtro del SQL solo
 * se puede comprobar aqui</b>, y es el que decide de verdad, porque es el
 * numero que el caso de uso compara contra el techo de cuatro.
 *
 * <p>
 * Lo mismo vale para la ventana de dos semanas: el {@code >= :since} del JPQL
 * no lo ve ningun doble.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPaymentAttemptRepository — intentos de cobro contra MySQL real")
class PaymentAttemptPersistenceIT extends AbstractDataJpaTest {

    private static final Long DOCUMENTO = 8400L;
    private static final Long OTRO_DOCUMENTO = 8401L;
    private static final Long DOCUMENTO_AJENO = 8402L;
    private static final Long MEDIO_DE_PAGO = 8410L;

    /**
     * El "ahora" del escenario. Fijo: la ventana se mide contra el, no contra hoy.
     */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 12, 0, 0);
    private static final LocalDateTime HACE_DOS_SEMANAS = AHORA.minus(PaymentAttempt.RETRY_WINDOW);

    @Autowired
    private JpaPaymentAttemptRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        // Periodos DISTINTOS entre los dos documentos de la misma empresa.
        // uq_sbd_recurring_cycle es UNIQUE sobre (recurring_cycle_marker,
        // period_start, period_end) y el marcador vale el id del contrato en toda
        // factura recurrente: mismo contrato y mismo periodo colisionan. El de la
        // empresa ajena cuelga de OTRA_SUBSCRIPTION_ID, asi que puede repetir mes.
        documento(DOCUMENTO, "FV-INTENTO-0001", SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                "2026-03-01", "2026-03-31");
        documento(OTRO_DOCUMENTO, "FV-INTENTO-0002", SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, "2026-04-01", "2026-04-30");
        documento(DOCUMENTO_AJENO, "FV-INTENTO-0003", SchemaSeed.OTRA_COMPANY_ID,
                SchemaSeed.OTRA_SUBSCRIPTION_ID, "2026-03-01", "2026-03-31");
        medioDePago(MEDIO_DE_PAGO, SchemaSeed.COMPANY_ID);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el intento y lo recupera con cada instante e importe en su sitio")
        void guarda_el_intento_y_lo_recupera_campo_a_campo() {
            PaymentAttempt guardado = repository.save(PaymentAttempt.attempted(
                    SchemaSeed.COMPANY_ID, DOCUMENTO, MEDIO_DE_PAGO, 1, "wompi",
                    new BigDecimal("119000.00"), "insufficient_funds", DeclineKind.SOFT,
                    LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDateTime.of(2026, 3, 8, 6, 0, 0),
                    LocalDateTime.of(2026, 3, 5, 14, 30, 20)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getBillingDocumentId()).isEqualTo(DOCUMENTO);
                        assertThat(recuperado.getPaymentMethodId()).isEqualTo(MEDIO_DE_PAGO);
                        assertThat(recuperado.getAttemptNumber()).isEqualTo(1);
                        assertThat(recuperado.getGateway()).isEqualTo("wompi");
                        assertThat(recuperado.getRequestedAmount())
                                .isEqualByComparingTo("119000.00");
                        assertThat(recuperado.getGatewayDeclineCode())
                                .isEqualTo("insufficient_funds");
                        assertThat(recuperado.getDeclineKind()).isEqualTo(DeclineKind.SOFT);
                        // Tres instantes distintos: intentado, siguiente y creado. Si el
                        // mapper cruzara dos, esto cae.
                        assertThat(recuperado.getAttemptedAt())
                                .isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
                        assertThat(recuperado.getNextAttemptAt())
                                .isEqualTo(LocalDateTime.of(2026, 3, 8, 6, 0, 0));
                        assertThat(recuperado.getCreatedDate())
                                .isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 20));
                        assertThat(recuperado.getVersion()).isZero();
                    });
        }

        @Test
        @DisplayName("un fallo propio se guarda sin medio de pago y sin codigo de la pasarela")
        void un_fallo_propio_se_guarda_sin_medio_y_sin_codigo() {
            // Un CONFIGURATION puede rebotar antes de llegar a usar medio alguno: la
            // pasarela no llego a decidir nada, asi que no hay codigo que guardar.
            // chk_payment_attempts_declined_by_gateway lo permite solo para esta clase.
            PaymentAttempt guardado = repository.save(PaymentAttempt.attempted(
                    SchemaSeed.COMPANY_ID, DOCUMENTO, null, 1, "wompi", new BigDecimal("119000.00"),
                    null, DeclineKind.CONFIGURATION, AHORA, null, AHORA));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getPaymentMethodId()).isNull();
                        assertThat(recuperado.getGatewayDeclineCode()).isNull();
                        assertThat(recuperado.consumesCustomerAttempts()).isFalse();
                    });
        }

        @Test
        @DisplayName("reprogramar mueve la fecha del siguiente intento y sube la version")
        void reprogramar_mueve_la_fecha_y_sube_la_version() {
            PaymentAttempt guardado = repository.save(intento(1, DeclineKind.SOFT, AHORA, null));
            entityManager.flush();
            entityManager.clear();

            PaymentAttempt recuperado = repository
                    .findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID).orElseThrow();
            recuperado.reschedule(AHORA.plusDays(3));
            repository.save(recuperado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(despues -> {
                        assertThat(despues.getNextAttemptAt()).isEqualTo(AHORA.plusDays(3));
                        // El ciclo leer-modificar-guardar es el unico camino que @Version
                        // protege, y aqui se comprueba que de verdad pasa por el.
                        assertThat(despues.getVersion()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("Presupuesto de reintentos")
    class PresupuestoDeReintentos {

        @Test
        @DisplayName("un fallo propio NO gasta presupuesto del cliente aunque este en la ventana")
        void un_fallo_propio_no_gasta_presupuesto() {
            repository.save(intento(1, DeclineKind.SOFT, AHORA.minusDays(1), null));
            repository.save(intento(2, DeclineKind.CONFIGURATION, AHORA.minusDays(1), null));
            repository.save(intento(3, DeclineKind.CONFIGURATION, AHORA.minusHours(2), null));
            entityManager.flush();

            // Tres intentos en la ventana, dos de ellos culpa nuestra. El contador
            // tiene que decir UNO. Si el filtro del SQL desapareciera diria tres, y
            // el cliente se quedaria sin reintentos por una credencial mal puesta.
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(1);
        }

        @Test
        @DisplayName("un rechazo duro si gasta presupuesto: no es un fallo nuestro")
        void un_rechazo_duro_si_gasta_presupuesto() {
            repository.save(intento(1, DeclineKind.HARD, AHORA.minusDays(1), null));
            entityManager.flush();

            // Solo CONFIGURATION esta exento. Un HARD es del cliente —tarjeta perdida,
            // autorizacion revocada— y cuenta, aunque no se reintente.
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(1);
        }

        @Test
        @DisplayName("cuatro intentos imputables llenan el techo que fija el dominio")
        void cuatro_intentos_imputables_llenan_el_techo() {
            repository.save(intento(1, DeclineKind.SOFT, AHORA.minusDays(10), null));
            repository.save(intento(2, DeclineKind.SOFT, AHORA.minusDays(7), null));
            repository.save(intento(3, DeclineKind.SOFT, AHORA.minusDays(3), null));
            repository.save(intento(4, DeclineKind.SOFT, AHORA.minusDays(1), null));
            entityManager.flush();

            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(PaymentAttempt.MAX_SOFT_ATTEMPTS);
        }

        @Test
        @DisplayName("lo anterior a la ventana ya no cuenta, y el borde exacto si cuenta")
        void lo_anterior_a_la_ventana_ya_no_cuenta() {
            repository.save(intento(1, DeclineKind.SOFT, HACE_DOS_SEMANAS.minusSeconds(1), null));
            repository.save(intento(2, DeclineKind.SOFT, HACE_DOS_SEMANAS, null));
            entityManager.flush();

            // Un segundo separa los dos. El JPQL usa >=, asi que el del borde entra y
            // el de un segundo antes no: si alguien cambiara el >= por un >, el
            // presupuesto se relajaria en silencio justo en el limite.
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(1);
        }

        @Test
        @DisplayName("el presupuesto es por documento y por empresa, no global")
        void el_presupuesto_es_por_documento_y_por_empresa() {
            repository.save(intento(1, DeclineKind.SOFT, AHORA.minusDays(1), null));
            repository.save(intentoDe(SchemaSeed.COMPANY_ID, OTRO_DOCUMENTO, 1, DeclineKind.SOFT,
                    AHORA.minusDays(1), null));
            repository.save(intentoDe(SchemaSeed.OTRA_COMPANY_ID, DOCUMENTO_AJENO, 1,
                    DeclineKind.SOFT, AHORA.minusDays(1), null));
            entityManager.flush();

            // Tres intentos vivos en la ventana y cada contador ve exactamente el suyo.
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(1);
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, OTRO_DOCUMENTO,
                    HACE_DOS_SEMANAS)).isEqualTo(1);
            // Y el documento de la otra empresa no se ve desde aqui ni aunque se
            // acierte con su id.
            assertThat(repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO_AJENO,
                    HACE_DOS_SEMANAS)).isZero();
        }

        @ParameterizedTest
        @EnumSource(DeclineKind.class)
        @DisplayName("cada clase de rechazo decide sola si gasta presupuesto, y el SQL coincide")
        void cada_clase_decide_sola_si_gasta_presupuesto(DeclineKind clase) {
            PaymentAttempt guardado = repository.save(intento(1, clase, AHORA.minusDays(1), null));
            entityManager.flush();

            // El dominio y el SQL tienen que dar la MISMA respuesta para las tres
            // clases. Una rama nueva en el enum sin su rama en el contador rompe aqui.
            int contado = repository.countRetryableSince(SchemaSeed.COMPANY_ID, DOCUMENTO,
                    HACE_DOS_SEMANAS);
            assertThat(contado == 1).as("el SQL cuenta el intento de clase %s", clase)
                    .isEqualTo(guardado.consumesCustomerAttempts());
        }
    }

    @Nested
    @DisplayName("Consecutivo")
    class Consecutivo {

        @Test
        @DisplayName("sin intentos previos no hay consecutivo gastado")
        void sin_intentos_previos_no_hay_consecutivo() {
            assertThat(repository.findMaxAttemptNumber(SchemaSeed.COMPANY_ID, DOCUMENTO)).isEmpty();
        }

        @Test
        @DisplayName("devuelve el mayor gastado sobre ese documento y no el de al lado")
        void devuelve_el_mayor_gastado_sobre_ese_documento() {
            repository.save(intento(1, DeclineKind.SOFT, AHORA.minusDays(3), null));
            repository.save(intento(2, DeclineKind.SOFT, AHORA.minusDays(2), null));
            repository.save(intentoDe(SchemaSeed.COMPANY_ID, OTRO_DOCUMENTO, 9, DeclineKind.SOFT,
                    AHORA.minusDays(1), null));
            entityManager.flush();

            // El 9 del documento vecino no debe contaminar: si el max perdiera su
            // filtro por documento, el siguiente consecutivo de DOCUMENTO seria 10.
            assertThat(repository.findMaxAttemptNumber(SchemaSeed.COMPANY_ID, DOCUMENTO))
                    .contains(2);
            assertThat(repository.findMaxAttemptNumber(SchemaSeed.COMPANY_ID, OTRO_DOCUMENTO))
                    .contains(9);
        }

        @Test
        @DisplayName("repetir el consecutivo sobre el mismo documento lo para uq_payment_attempts_number")
        void repetir_el_consecutivo_lo_para_la_unicidad() {
            repository.save(intento(1, DeclineKind.SOFT, AHORA.minusDays(3), null));
            entityManager.flush();

            // Pasarela e importe distintos: lo unico repetido es el par
            // (documento, numero de intento).
            EngineConstraint.assertViolates("uq_payment_attempts_number", () -> {
                repository.save(PaymentAttempt.attempted(SchemaSeed.COMPANY_ID, DOCUMENTO,
                        MEDIO_DE_PAGO, 1, "otra-pasarela", new BigDecimal("1.00"), "do_not_honor",
                        DeclineKind.SOFT, AHORA.minusDays(2), null, AHORA));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un rechazo duro con reintento programado lo para chk_payment_attempts_hard_has_no_retry")
        void un_rechazo_duro_con_reintento_lo_para_el_check() {
            // El dominio ya lo impide, asi que la unica forma de comprobar que la base
            // tambien lo cuida es escribir la fila cruda.
            EngineConstraint.assertViolates("chk_payment_attempts_hard_has_no_retry",
                    () -> insertarIntentoCrudo(1, "HARD", "lost_card", AHORA, AHORA.plusDays(2)));
        }

        @Test
        @DisplayName("un rechazo de la pasarela sin codigo lo para chk_payment_attempts_declined_by_gateway")
        void un_rechazo_de_la_pasarela_sin_codigo_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_payment_attempts_declined_by_gateway",
                    () -> insertarIntentoCrudo(1, "SOFT", null, AHORA, null));
        }

        @Test
        @DisplayName("un reintento anterior al propio intento lo para chk_payment_attempts_retry_is_later")
        void un_reintento_anterior_al_intento_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_payment_attempts_retry_is_later",
                    () -> insertarIntentoCrudo(1, "SOFT", "insufficient_funds", AHORA,
                            AHORA.minusHours(1)));
        }
    }

    @Nested
    @DisplayName("Cola de reintentos y listados")
    class ColaDeReintentosYListados {

        @Test
        @DisplayName("la cola trae lo vencido de todas las empresas, lo mas antiguo primero")
        void la_cola_trae_lo_vencido_de_todas_las_empresas() {
            PaymentAttempt vencidoTarde = repository
                    .save(intento(1, DeclineKind.SOFT, AHORA.minusDays(5), AHORA.minusHours(1)));
            PaymentAttempt vencidoPronto = repository.save(intentoDe(SchemaSeed.OTRA_COMPANY_ID,
                    DOCUMENTO_AJENO, 1, DeclineKind.SOFT, AHORA.minusDays(6), AHORA.minusDays(2)));
            // Aun no le toca.
            repository.save(intento(2, DeclineKind.SOFT, AHORA.minusDays(4), AHORA.plusDays(1)));
            // Y este no tiene siguiente: un rechazo duro no vuelve a la cola jamas.
            repository.save(intentoDe(SchemaSeed.COMPANY_ID, OTRO_DOCUMENTO, 1, DeclineKind.HARD,
                    AHORA.minusDays(3), null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllDueForRetry(AHORA, 0, 20).content())
                    .extracting(PaymentAttempt::getId)
                    .containsExactly(vencidoPronto.getId(), vencidoTarde.getId());
        }

        @Test
        @DisplayName("el historial de una factura se lee del primer intento al ultimo")
        void el_historial_de_una_factura_va_en_ascendente() {
            PaymentAttempt tercero = repository
                    .save(intento(3, DeclineKind.SOFT, AHORA.minusDays(1), null));
            PaymentAttempt primero = repository
                    .save(intento(1, DeclineKind.SOFT, AHORA.minusDays(3), null));
            PaymentAttempt segundo = repository
                    .save(intento(2, DeclineKind.SOFT, AHORA.minusDays(2), null));
            entityManager.flush();
            entityManager.clear();

            // Se guardan desordenados a proposito: «se intento tres veces» se lee del
            // primero al ultimo, no al reves que el resto de listados de este slice.
            assertThat(repository
                    .findAllByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID, DOCUMENTO, 0, 20)
                    .content()).extracting(PaymentAttempt::getAttemptNumber)
                    .containsExactly(1, 2, 3);
            assertThat(repository
                    .findAllByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID, DOCUMENTO, 0, 20)
                    .content()).extracting(PaymentAttempt::getId)
                    .containsExactly(primero.getId(), segundo.getId(), tercero.getId());
        }

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            PaymentAttempt propio = repository.save(intento(1, DeclineKind.SOFT, AHORA, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }
    }

    // --- andamio ------------------------------------------------------------

    private PaymentAttempt intento(int numero, DeclineKind clase, LocalDateTime intentadoEl,
            LocalDateTime siguiente) {
        return intentoDe(SchemaSeed.COMPANY_ID, DOCUMENTO, numero, clase, intentadoEl, siguiente);
    }

    /**
     * El medio de pago solo se enlaza cuando la empresa es la propia: la FK es
     * compuesta {@code (company_id, payment_method_id)} y el medio sembrado es de
     * {@code COMPANY_ID}.
     */
    private PaymentAttempt intentoDe(Long companyId, Long documentoId, int numero,
            DeclineKind clase, LocalDateTime intentadoEl, LocalDateTime siguiente) {
        Long medio = SchemaSeed.COMPANY_ID.equals(companyId) ? MEDIO_DE_PAGO : null;
        String codigo = clase == DeclineKind.CONFIGURATION ? null : "insufficient_funds";
        return PaymentAttempt.attempted(companyId, documentoId, medio, numero, "wompi",
                new BigDecimal("119000.00"), codigo, clase, intentadoEl, siguiente, AHORA);
    }

    private void insertarIntentoCrudo(int numero, String clase, String codigo,
            LocalDateTime intentadoEl, LocalDateTime siguiente) {
        entityManager.createNativeQuery("""
                INSERT INTO payment_attempts (company_id, billing_document_id, payment_method_id,
                                              attempt_number, gateway, requested_amount,
                                              gateway_decline_code, decline_kind, attempted_at,
                                              next_attempt_at, created_date, version)
                VALUES (:companyId, :documento, :medio, :numero, 'wompi', 119000.00, :codigo,
                        :clase, :intentadoEl, :siguiente, :creadoEl, 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("documento", DOCUMENTO).setParameter("medio", MEDIO_DE_PAGO)
                .setParameter("numero", numero).setParameter("codigo", codigo)
                .setParameter("clase", clase).setParameter("intentadoEl", intentadoEl)
                .setParameter("siguiente", siguiente).setParameter("creadoEl", AHORA)
                .executeUpdate();
    }

    private void documento(Long id, String numero, Long companyId, Long subscriptionId,
            String inicio, String fin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', 'RECURRING_CYCLE',
                        :inicio, :fin, 'DRAFT', 100000.00, 19000.00, 119000.00,
                        0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("inicio", inicio).setParameter("fin", fin).executeUpdate();
    }

    /**
     * Medio PSE: {@code chk_subscription_payment_methods_card_shape} exige que
     * {@code brand}, {@code last_four} y {@code expires_on} vayan los tres vacios.
     * {@code default_marker} es {@code GENERATED ALWAYS} y no se nombra.
     */
    private void medioDePago(Long id, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_payment_methods (id, company_id, method_kind, gateway,
                                                          token, mandate_status, mandate_evidence,
                                                          authorized_at, is_default, created_date,
                                                          enabled, version)
                VALUES (:id, :companyId, 'PSE', 'wompi', 'tok-intentos-8410', 'ACTIVE',
                        'Mandato firmado en el portal', '2026-01-05 09:00:00', false, NOW(),
                        true, 0)
                """).setParameter("id", id).setParameter("companyId", companyId).executeUpdate();
    }
}
