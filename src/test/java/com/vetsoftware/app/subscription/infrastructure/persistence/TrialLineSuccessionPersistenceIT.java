package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * {@code JpaTrialLineSuccessionPort.transitionIfOpen} contra MySQL real: el
 * vencimiento natural de una prueba, con su empresa, su ventana, su concesión y
 * su línea {@code TRIAL} sembradas como llegarían de verdad —no por un doble—
 * porque la fila que se cierra tiene que respetar
 * {@code fk_subscription_items_trial_grant} y
 * {@code uq_subscription_items_current} a la vez.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaTrialLineSuccessionPort — el vencimiento natural de una prueba contra MySQL real")
class TrialLineSuccessionPersistenceIT extends AbstractDataJpaTest {

    private static final Long ARTICULO_TRIAL_ID = 9800L;
    private static final LocalDate CONCEDIDA_EL = LocalDate.of(2026, 1, 1);
    private static final int DIAS_CONCEDIDOS = 14;
    private static final LocalDate FIN_PRUEBA = CONCEDIDA_EL.plusDays(DIAS_CONCEDIDOS - 1L);

    @Autowired
    private JpaTrialLineSuccessionPort port;
    @Autowired
    private TrialSubscriptionItemJpaRepository trialRepository;
    @Autowired
    private JpaSubscriptionItemRepository subscriptionItemRepository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;
    @Autowired
    private SubscriptionJpaRepository subscriptionJpaRepository;
    @Autowired
    private SubscriptionItemJpaMapper mapper;
    @PersistenceContext
    private EntityManager entityManager;

    private Long lineaTrialId;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        articuloTrial();
        Long ventanaId = ventanaAbierta();
        concesion(ventanaId);
        lineaTrialId = lineaTrial();
        entityManager.flush();
        entityManager.clear();
    }

    private void articuloTrial() {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, item_type, structural_minimum, min_quantity,
                                                           max_quantity, sort_order, status, trial_eligibility,
                                                           default_trial_days, trial_outcome, service_nature,
                                                           created_date, enabled, version)
                                VALUES (:id, 'TRIAL_ART', 'Modulo en prueba', 'MODULE', false, 1, 1, 30, 'ACTIVE',
                                        'ELIGIBLE', 14, 'LIMITED', 'SOFTWARE_LICENSING', '2026-01-01 00:00:00', true, 0)
                                """)
                .setParameter("id", ARTICULO_TRIAL_ID).executeUpdate();
    }

    /** {@code origin = SIGNUP} porque {@code source_quote_id} es {@code NULL}. */
    private Long ventanaAbierta() {
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_windows (company_id, start_date, end_date, window_days,
                                                   source_quote_id, origin, closed_at, created_date,
                                                   version)
                VALUES (:companyId, :inicio, :fin, 30, NULL, 'SIGNUP', NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("inicio", CONCEDIDA_EL).setParameter("fin", CONCEDIDA_EL.plusDays(29))
                .executeUpdate();
        entityManager.flush();
        return ((Number) entityManager
                .createNativeQuery(
                        """
                                SELECT id FROM company_trial_windows WHERE company_id = :companyId AND origin = 'SIGNUP'
                                """)
                .setParameter("companyId", SchemaSeed.COMPANY_ID).getSingleResult()).longValue();
    }

    /** {@code origin = SIGNUP}: ni cotización ni otrosí la concedieron. */
    private void concesion(Long ventanaId) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO company_trial_grants (company_id, catalog_item_id, trial_window_id,
                                                                  trial_window_end_date, granted_on, days_granted,
                                                                  trial_end_date, policy_trial_days,
                                                                  policy_trial_outcome, source_quote_id,
                                                                  granting_amendment_id, origin, consumed_at, outcome,
                                                                  created_date, version)
                                VALUES (:companyId, :itemId, :ventanaId, :finVentana, :concedidaEl, :dias, :finPrueba,
                                        :dias, 'LIMITED', NULL, NULL, 'SIGNUP', NULL, NULL, NOW(), 0)
                                """)
                .setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("itemId", ARTICULO_TRIAL_ID).setParameter("ventanaId", ventanaId)
                .setParameter("finVentana", CONCEDIDA_EL.plusDays(29))
                .setParameter("concedidaEl", CONCEDIDA_EL).setParameter("dias", DIAS_CONCEDIDOS)
                .setParameter("finPrueba", FIN_PRUEBA).executeUpdate();
    }

    private Long lineaTrial() {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(SchemaSeed.COMPANY_ID);
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(SchemaSeed.SUBSCRIPTION_ID);
        SubscriptionItem trial = SubscriptionItem.open(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, ARTICULO_TRIAL_ID, "TRIAL_ART", "Modulo en prueba",
                SubscriptionItemType.MODULE, null, 1, null, 0, TaxTreatment.TAXED, 1,
                new BigDecimal("80000.00"), BigDecimal.ZERO, BigDecimal.ZERO, false,
                new BigDecimal("19.00"), EffectivePeriod.openFrom(CONCEDIDA_EL), ItemOrigin.INITIAL,
                null, "TRIAL", "ELIGIBLE", DIAS_CONCEDIDOS, FIN_PRUEBA, "SELF_SERVICE");
        return trialRepository.save(mapper.toJpa(trial, company, subscription)).getId();
    }

    @Nested
    @DisplayName("Vencimiento natural")
    class VencimientoNatural {

        @Test
        @DisplayName("cierra la prueba al día siguiente de su fin y abre la sucesora con el desenlace")
        void cierra_la_prueba_y_abre_la_sucesora_con_el_desenlace() {
            boolean transicionada = port.transitionIfOpen(SchemaSeed.COMPANY_ID, ARTICULO_TRIAL_ID,
                    FIN_PRUEBA, "PAID");
            entityManager.clear();

            assertThat(transicionada).isTrue();
            assertThat(subscriptionItemRepository.findByIdAndCompanyId(lineaTrialId,
                    SchemaSeed.COMPANY_ID)).get()
                    .satisfies(cerrada -> assertThat(cerrada.getPeriod().to())
                            .isEqualTo(FIN_PRUEBA.plusDays(1)));
            assertThat(subscriptionItemRepository.findOpenByCatalogItemId(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, ARTICULO_TRIAL_ID)).get().satisfies(sucesora -> {
                        assertThat(sucesora.getId()).isNotEqualTo(lineaTrialId);
                        assertThat(sucesora.getSucceedsItemId()).isEqualTo(lineaTrialId);
                        assertThat(sucesora.getChargeMode()).isEqualTo("PAID");
                        assertThat(sucesora.getOrigin()).isEqualTo(ItemOrigin.INITIAL);
                        assertThat(sucesora.getTrialEndDate()).isNull();
                        assertThat(sucesora.getItemCode()).isEqualTo("TRIAL_ART");
                        assertThat(sucesora.getUnitAmount()).isEqualByComparingTo("80000.00");
                        assertThat(sucesora.getPeriod().from()).isEqualTo(FIN_PRUEBA.plusDays(1));
                        assertThat(sucesora.getPeriod().isOpen()).isTrue();
                    });
        }

        @Test
        @DisplayName("es idempotente: la segunda llamada no encuentra prueba abierta y no escribe")
        void es_idempotente_la_segunda_llamada_no_escribe_nada() {
            assertThat(port.transitionIfOpen(SchemaSeed.COMPANY_ID, ARTICULO_TRIAL_ID, FIN_PRUEBA,
                    "PAID")).isTrue();
            entityManager.clear();

            assertThat(port.transitionIfOpen(SchemaSeed.COMPANY_ID, ARTICULO_TRIAL_ID, FIN_PRUEBA,
                    "PAID")).isFalse();
            assertThat(subscriptionItemRepository.findAllBySubscriptionIdAndCompanyId(
                    SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(SubscriptionItem::getCatalogItemId)
                    .filteredOn(ARTICULO_TRIAL_ID::equals).hasSize(2);
        }

        @Test
        @DisplayName("sin prueba abierta para ese artículo, en esa empresa, no hace nada")
        void sin_prueba_abierta_no_hace_nada() {
            assertThat(port.transitionIfOpen(SchemaSeed.OTRA_COMPANY_ID, ARTICULO_TRIAL_ID,
                    FIN_PRUEBA, "PAID")).isFalse();
        }
    }
}
