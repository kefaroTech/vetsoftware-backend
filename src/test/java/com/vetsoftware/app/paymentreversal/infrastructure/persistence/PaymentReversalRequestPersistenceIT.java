package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import com.vetsoftware.app.shared.pagination.PageResult;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * El expediente de reversion contra MySQL real.
 *
 * <p>
 * <b>Aqui se prueban las dos cosas que ningun test con el repositorio mockeado
 * puede ver.</b> La primera son las <b>cuatro fechas</b> —conocimiento, queja,
 * aviso al emisor y plazo—: viajan por el mapper como cuatro
 * {@code LocalDateTime} indistinguibles entre si, y un par intercambiado
 * compila, pasa el mapeo y solo se nota el dia que hay que alegar que la
 * reclamacion llego fuera de plazo. Por eso cada una lleva una hora, un minuto
 * y un segundo distintos y reconocibles: si dos se cruzan, el caso se pone
 * rojo.
 *
 * <p>
 * La segunda son <b>las barandillas del changeset 322</b>. Las invariantes del
 * dominio ya rechazan un plazo anterior a la queja o una reclamacion de
 * consumidor sin causal, asi que para llegar al motor hay que escribir la fila
 * <b>por SQL nativo, saltandose el dominio a proposito</b>: eso es exactamente
 * lo que hace una carga de datos, una migracion o un {@code @Query} de
 * {@code UPDATE} el dia que alguien lo escriba. Si el {@code CHECK} no
 * estuviera —o estuviera con la redaccion que evalua a {@code NULL} y por tanto
 * no restringe nada, que es la trampa documentada en el propio changeset— la
 * fila prohibida entraria sin ruido.
 *
 * <p>
 * Cada caso nombra la restriccion que espera con
 * {@link EngineConstraint#assertViolates}, y las filas se preparan para que
 * <b>solo</b> pueda saltar esa: clave foranea satisfecha y unicidad libre
 * cuando lo que se prueba es un {@code CHECK}, y {@code CHECK}s satisfechos
 * cuando lo que se prueba es la clave foranea compuesta.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPaymentReversalRequestRepository — expedientes de reversion contra MySQL real")
class PaymentReversalRequestPersistenceIT extends AbstractDataJpaTest {

    /**
     * Rango propio de esta rodaja: {@link SchemaSeed} no siembra ninguna tabla de
     * dinero, asi que los pagos que sostienen {@code fk_prr_payment} los pone este
     * test y no chocan con los ids de nadie.
     */
    private static final Long PAGO_1 = 8_001L;
    private static final Long PAGO_2 = 8_002L;
    private static final Long PAGO_3 = 8_003L;
    private static final Long PAGO_4 = 8_004L;
    private static final Long PAGO_5 = 8_005L;
    private static final Long PAGO_6 = 8_006L;
    private static final Long PAGO_AJENO_1 = 8_101L;

    /** Cuatro instantes distintos: intercambiar dos cualesquiera rompe el caso. */
    private static final LocalDateTime CONOCIMIENTO = LocalDateTime.of(2026, 3, 1, 8, 15, 11);
    private static final LocalDateTime QUEJA = LocalDateTime.of(2026, 3, 5, 9, 26, 22);
    private static final LocalDateTime AVISO_AL_EMISOR = LocalDateTime.of(2026, 3, 7, 10, 37, 33);
    private static final LocalDateTime PLAZO = LocalDateTime.of(2026, 4, 19, 11, 48, 44);
    private static final LocalDateTime NACIMIENTO = LocalDateTime.of(2026, 3, 5, 12, 59, 55);

    private static final LocalDateTime QUEJA_POSTERIOR = LocalDateTime.of(2026, 3, 9, 17, 3, 8);
    private static final LocalDateTime CORTE = LocalDateTime.of(2026, 4, 15, 0, 0, 0);

    @Autowired
    private JpaPaymentReversalRequestRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        pago(PAGO_1, SchemaSeed.COMPANY_ID);
        pago(PAGO_2, SchemaSeed.COMPANY_ID);
        pago(PAGO_3, SchemaSeed.COMPANY_ID);
        pago(PAGO_4, SchemaSeed.COMPANY_ID);
        pago(PAGO_5, SchemaSeed.COMPANY_ID);
        pago(PAGO_6, SchemaSeed.COMPANY_ID);
        pago(PAGO_AJENO_1, SchemaSeed.OTRA_COMPANY_ID);
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda y recupera las cuatro fechas en su sitio: conocimiento, queja,"
                + " aviso al emisor y plazo no se pueden intercambiar")
        void guarda_y_recupera_las_cuatro_fechas_en_su_sitio() {
            PaymentReversalRequest guardado = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(leido -> {
                        assertThat(leido.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(leido.getPaymentId()).isEqualTo(PAGO_1);
                        assertThat(leido.getOrigin()).isEqualTo(ReversalOrigin.CONSUMER_CLAIM);
                        assertThat(leido.getCausal())
                                .isEqualTo(ReversalCausal.PRODUCT_NOT_RECEIVED);
                        assertThat(leido.getConsumerDetermination())
                                .isEqualTo(ConsumerDetermination.CONSUMER);
                        assertThat(leido.getConsumerBecameAwareAt()).isEqualTo(CONOCIMIENTO);
                        assertThat(leido.getClaimReceivedAt()).isEqualTo(QUEJA);
                        assertThat(leido.getIssuerNotifiedAt()).isEqualTo(AVISO_AL_EMISOR);
                        assertThat(leido.getDeadlineAt()).isEqualTo(PLAZO);
                        assertThat(leido.getClaimEvidenceRef()).isEqualTo("EV-QUEJA-" + PAGO_1);
                        assertThat(leido.getCreatedDate()).isEqualTo(NACIMIENTO);
                        assertThat(leido.getOutcome()).isNull();
                        assertThat(leido.getAppliedAmount()).isNull();
                        assertThat(leido.getAcknowledgementRef()).isNull();
                        assertThat(leido.getOppositionGround()).isNull();
                    });
        }

        @Test
        @DisplayName("guarda el lado propio del expediente —acuse, oposicion y desenlace— con el"
                + " importe aplicado exacto")
        void guarda_el_lado_propio_del_expediente() {
            PaymentReversalRequest abierto = expediente(SchemaSeed.COMPANY_ID, PAGO_2, QUEJA,
                    PLAZO);
            abierto.acknowledge("ACU-0001", LocalDateTime.of(2026, 3, 6, 13, 5, 6));
            abierto.oppose(OppositionGround.CAUSAL_NOT_REPORTED, "EV-OPOSICION-0001",
                    LocalDateTime.of(2026, 3, 8, 14, 16, 17));
            abierto.resolve(ReversalOutcome.PARTIALLY_ACCEPTED, new BigDecimal("137500.75"), null);
            PaymentReversalRequest guardado = repository.save(abierto);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(leido -> {
                        assertThat(leido.getAcknowledgementRef()).isEqualTo("ACU-0001");
                        assertThat(leido.getAcknowledgedAt())
                                .isEqualTo(LocalDateTime.of(2026, 3, 6, 13, 5, 6));
                        assertThat(leido.getOppositionGround())
                                .isEqualTo(OppositionGround.CAUSAL_NOT_REPORTED);
                        assertThat(leido.getOppositionEvidenceRef()).isEqualTo("EV-OPOSICION-0001");
                        assertThat(leido.getOpposedAt())
                                .isEqualTo(LocalDateTime.of(2026, 3, 8, 14, 16, 17));
                        assertThat(leido.getOutcome())
                                .isEqualTo(ReversalOutcome.PARTIALLY_ACCEPTED);
                        assertThat(leido.getAppliedAmount()).isEqualByComparingTo("137500.75");
                        assertThat(leido.getResultingRefundId()).isNull();
                    });
        }

        @Test
        @DisplayName("recupera el expediente por el pago que reversa")
        void recupera_el_expediente_por_el_pago_que_reversa() {
            PaymentReversalRequest guardado = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_3, QUEJA, PLAZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByCompanyIdAndPaymentId(SchemaSeed.COMPANY_ID, PAGO_3)).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.getPaymentId()).isEqualTo(PAGO_3);
                    });
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el expediente no se le sirve a otra empresa aunque acierte el id")
        void el_expediente_no_se_le_sirve_a_otra_empresa() {
            PaymentReversalRequest guardado = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByCompanyIdAndPaymentId(SchemaSeed.OTRA_COMPANY_ID, PAGO_1))
                    .isEmpty();
        }

        @Test
        @DisplayName("el listado de una empresa no ve los expedientes de la otra")
        void el_listado_de_una_empresa_no_ve_los_de_la_otra() {
            PaymentReversalRequest propio = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            PaymentReversalRequest ajeno = repository
                    .save(expediente(SchemaSeed.OTRA_COMPANY_ID, PAGO_AJENO_1, QUEJA, PLAZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(PaymentReversalRequest::getId).containsExactly(propio.getId());
            assertThat(repository.findAllByCompanyId(SchemaSeed.OTRA_COMPANY_ID, 0, 20).content())
                    .extracting(PaymentReversalRequest::getId).containsExactly(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("Barandillas del motor")
    class BarandillasDelMotor {

        @Test
        @DisplayName("un segundo expediente sobre el mismo pago lo para"
                + " uq_payment_reversal_requests_payment: una reversion por pago")
        void un_segundo_expediente_sobre_el_mismo_pago_lo_para_la_unicidad() {
            repository.save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_payment_reversal_requests_payment", () -> {
                repository.save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA_POSTERIOR, PLAZO));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("un plazo anterior a la queja lo para chk_prr_deadline aunque la fila entre"
                + " por SQL nativo saltandose las invariantes del dominio")
        void un_plazo_anterior_a_la_queja_lo_para_el_check_de_plazo() {
            EngineConstraint.assertViolates("chk_prr_deadline", () -> {
                expedienteCrudo(8_201L, SchemaSeed.COMPANY_ID, PAGO_5, "'FRAUD'", QUEJA,
                        QUEJA.minusDays(1));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("una reclamacion de consumidor sin causal la para chk_prr_causal_required:"
                + " la redaccion que evaluaba a NULL no restringia nada")
        void una_reclamacion_de_consumidor_sin_causal_la_para_el_check_de_causal() {
            EngineConstraint.assertViolates("chk_prr_causal_required", () -> {
                expedienteCrudo(8_202L, SchemaSeed.COMPANY_ID, PAGO_6, "NULL", QUEJA, PLAZO);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("un expediente sobre el pago de otra empresa lo para fk_prr_payment: la clave"
                + " foranea es compuesta (company_id, id) y cruzar de tenant no llega a la fila")
        void un_pago_de_otra_empresa_lo_para_la_clave_foranea_compuesta() {
            EngineConstraint.assertViolates("fk_prr_payment", () -> {
                expedienteCrudo(8_203L, SchemaSeed.COMPANY_ID, PAGO_AJENO_1, "'FRAUD'", QUEJA,
                        PLAZO);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Orden de los listados")
    class Orden {

        @Test
        @DisplayName("el listado de empresa va por queja descendente y desempata por id:"
                + " sin desempate dos expedientes del mismo instante salen dos veces o ninguna")
        void el_listado_de_empresa_ordena_por_queja_y_desempata_por_id() {
            PaymentReversalRequest antiguo = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            PaymentReversalRequest reciente = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_2, QUEJA_POSTERIOR, PLAZO));
            PaymentReversalRequest empatado = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_3, QUEJA, PLAZO));
            entityManager.flush();
            entityManager.clear();

            PageResult<PaymentReversalRequest> pagina = repository
                    .findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(PaymentReversalRequest::getId)
                    .containsExactly(reciente.getId(), empatado.getId(), antiguo.getId());
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("el listado de plataforma recorre todas las clinicas con el mismo orden")
        void el_listado_de_plataforma_recorre_todas_las_clinicas() {
            PaymentReversalRequest propio = repository
                    .save(expediente(SchemaSeed.COMPANY_ID, PAGO_1, QUEJA, PLAZO));
            PaymentReversalRequest ajeno = repository.save(
                    expediente(SchemaSeed.OTRA_COMPANY_ID, PAGO_AJENO_1, QUEJA_POSTERIOR, PLAZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).content())
                    .extracting(PaymentReversalRequest::getId)
                    .containsExactly(ajeno.getId(), propio.getId());
        }
    }

    @Nested
    @DisplayName("Barrido de plazos")
    class BarridoDePlazos {

        @Test
        @DisplayName("solo salen los que vencen antes del corte y siguen sin resolver, del que"
                + " antes vence al que despues, y cruzando clinicas a proposito")
        void solo_salen_los_no_resueltos_que_vencen_antes_del_corte() {
            PaymentReversalRequest venceDespues = repository.save(expediente(SchemaSeed.COMPANY_ID,
                    PAGO_1, QUEJA, LocalDateTime.of(2026, 4, 10, 6, 30, 1)));
            PaymentReversalRequest venceAntes = repository
                    .save(expediente(SchemaSeed.OTRA_COMPANY_ID, PAGO_AJENO_1, QUEJA,
                            LocalDateTime.of(2026, 4, 5, 6, 30, 2)));
            PaymentReversalRequest fueraDeVentana = repository.save(expediente(
                    SchemaSeed.COMPANY_ID, PAGO_2, QUEJA, LocalDateTime.of(2026, 5, 20, 6, 30, 3)));
            PaymentReversalRequest resuelto = resuelto(PAGO_4,
                    LocalDateTime.of(2026, 4, 1, 6, 30, 4));
            entityManager.flush();
            entityManager.clear();

            PageResult<PaymentReversalRequest> pagina = repository.findAllExpiringBefore(CORTE, 0,
                    20);

            assertThat(pagina.content()).extracting(PaymentReversalRequest::getId)
                    .containsExactly(venceAntes.getId(), venceDespues.getId())
                    .doesNotContain(fueraDeVentana.getId(), resuelto.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }
    }

    private PaymentReversalRequest resuelto(Long paymentId, LocalDateTime plazo) {
        PaymentReversalRequest abierto = expediente(SchemaSeed.COMPANY_ID, paymentId, QUEJA, plazo);
        abierto.resolve(ReversalOutcome.ACCEPTED, new BigDecimal("137500.75"), null);
        return repository.save(abierto);
    }

    private PaymentReversalRequest expediente(Long companyId, Long paymentId, LocalDateTime queja,
            LocalDateTime plazo) {
        return PaymentReversalRequest.open(companyId, paymentId, ReversalOrigin.CONSUMER_CLAIM,
                ReversalCausal.PRODUCT_NOT_RECEIVED, ConsumerDetermination.CONSUMER, CONOCIMIENTO,
                queja, AVISO_AL_EMISOR, "EV-QUEJA-" + paymentId, plazo, NACIMIENTO);
    }

    /**
     * El pago que sostiene {@code fk_prr_payment}. {@code gateway} y
     * {@code gateway_reference} van los dos vacios porque
     * {@code chk_subscription_payments_gateway_pair} los exige juntos o ninguno.
     */
    private void pago(Long id, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_payments (id, company_id, amount, currency,
                                                   payment_method, received_at, status,
                                                   refunded_amount, created_date, version)
                VALUES (:id, :companyId, 250000.00, 'COP', 'TRANSFER', '2026-03-01 07:00:00',
                        'CONFIRMED', 0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("companyId", companyId).executeUpdate();
    }

    /**
     * La fila escrita <b>sin pasar por el dominio</b>, que es la unica forma de
     * llegar a los {@code CHECK} del changeset 322: las invariantes de
     * {@code PaymentReversalRequest} rechazan estos valores mucho antes.
     *
     * <p>
     * La causal va como literal y no como parametro porque un {@code null} sin tipo
     * en una consulta nativa no se puede inferir.
     */
    private void expedienteCrudo(Long id, Long companyId, Long paymentId, String causal,
            LocalDateTime queja, LocalDateTime plazo) {
        entityManager.createNativeQuery("""
                INSERT INTO payment_reversal_requests (id, company_id, payment_id, origin, causal,
                                                       consumer_determination,
                                                       consumer_became_aware_at, claim_received_at,
                                                       issuer_notified_at, claim_evidence_ref,
                                                       deadline_at, created_date, version)
                VALUES (:id, :companyId, :paymentId, 'CONSUMER_CLAIM', %s, 'CONSUMER', NULL,
                        :queja, NULL, 'EV-CRUDO-0001', :plazo, NOW(6), 0)
                """.formatted(causal)).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("paymentId", paymentId).setParameter("queja", queja)
                .setParameter("plazo", plazo).executeUpdate();
    }
}
