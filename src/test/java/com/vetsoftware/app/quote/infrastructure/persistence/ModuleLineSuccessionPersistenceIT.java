package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort.CurrentLine;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort.LinePriceSnapshot;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.subscription.infrastructure.persistence.JpaSubscriptionItemRepository;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaMapper;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import com.vetsoftware.app.subscription.infrastructure.persistence.TrialSubscriptionItemJpaRepository;
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
 * {@code JpaModuleLineSuccessionPort} contra MySQL real: las consultas de
 * facturación que alimentan un cargo inmediato
 * ({@link JpaModuleLineSuccessionPort#findNextBillingDate}) y la sucesión
 * física de líneas, incluida la compra a mitad de una prueba —el camino que
 * {@code TrialLineSuccessionPersistenceIT} no cubre porque ahí quien cierra la
 * prueba es el barrido nocturno, no un cliente pagando hoy—.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaModuleLineSuccessionPort — sucesión de líneas contra MySQL real")
class ModuleLineSuccessionPersistenceIT extends AbstractDataJpaTest {

    private static final Long ARTICULO_EXTRA_ID = 9820L;
    private static final Long ARTICULO_TRIAL_ID = 9830L;
    private static final LocalDate CONCEDIDA_EL = LocalDate.of(2026, 1, 1);
    private static final int DIAS_CONCEDIDOS = 10;
    private static final LocalDate FIN_PRUEBA = CONCEDIDA_EL.plusDays(DIAS_CONCEDIDOS - 1L);

    @Autowired
    private JpaModuleLineSuccessionPort port;
    @Autowired
    private JpaSubscriptionItemRepository subscriptionItemRepository;
    @Autowired
    private TrialSubscriptionItemJpaRepository trialRepository;
    @Autowired
    private SubscriptionItemJpaMapper mapper;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;
    @Autowired
    private SubscriptionJpaRepository subscriptionJpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    private void insertarArticuloExtra(Long id, String code) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, item_type, structural_minimum, min_quantity,
                                                           max_quantity, sort_order, status, trial_eligibility,
                                                           default_trial_days, trial_outcome, service_nature,
                                                           created_date, enabled, version)
                                VALUES (:id, :code, 'Modulo extra', 'MODULE', false, 1, 1, 30, 'ACTIVE', 'ELIGIBLE',
                                        14, 'LIMITED', 'SOFTWARE_LICENSING', '2026-01-01 00:00:00', true, 0)
                                """)
                .setParameter("id", id).setParameter("code", code).executeUpdate();
    }

    /**
     * {@code origin = SIGNUP}: la línea de prueba de la compra a mitad de prueba.
     */
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

    @Nested
    @DisplayName("Las dos consultas de facturación")
    class Consultas {

        @Test
        @DisplayName("resuelve el contrato vigente y su próxima fecha de cobro, por empresa")
        void resuelve_el_contrato_vigente_y_su_proxima_fecha_de_cobro() {
            assertThat(port.findCurrentSubscriptionId(SchemaSeed.COMPANY_ID))
                    .contains(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(port.findNextBillingDate(SchemaSeed.COMPANY_ID))
                    .contains(LocalDate.of(2026, 2, 1));
            assertThat(port.findCurrentSubscriptionId(SchemaSeed.OTRA_COMPANY_ID))
                    .contains(SchemaSeed.OTRA_SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("sin contrato vigente para esa empresa, las dos consultas vuelven vacías")
        void sin_contrato_vigente_las_dos_vuelven_vacias() {
            assertThat(port.findCurrentSubscriptionId(-1L)).isEmpty();
            assertThat(port.findNextBillingDate(-1L)).isEmpty();
        }

        @Test
        @DisplayName("la línea abierta del núcleo se ve con su modo de cobro, y no se ve la de otra empresa")
        void la_linea_abierta_del_nucleo_se_ve_con_su_modo_de_cobro() {
            assertThat(port.findCurrentLine(SchemaSeed.COMPANY_ID, nucleo))
                    .contains(new CurrentLine(SchemaSeed.SUBSCRIPTION_ITEM_ID,
                            SchemaSeed.SUBSCRIPTION_ID, "PAID", null));
            assertThat(port.findCurrentLine(SchemaSeed.OTRA_COMPANY_ID, nucleo))
                    .contains(new CurrentLine(SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID,
                            SchemaSeed.OTRA_SUBSCRIPTION_ID, "PAID", null));
        }

        @Test
        @DisplayName("un artículo que la empresa no tiene contratado no tiene línea actual")
        void un_articulo_no_contratado_no_tiene_linea_actual() {
            assertThat(port.findCurrentLine(SchemaSeed.COMPANY_ID, -1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cerrar una línea")
    class CerrarLinea {

        @Test
        @DisplayName("escribe el effective_to y la línea deja de ser la actual")
        void escribe_el_effective_to_y_deja_de_ser_la_actual() {
            port.closeLine(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID,
                    LocalDate.of(2026, 6, 30));
            entityManager.clear();

            assertThat(subscriptionItemRepository
                    .findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ITEM_ID, SchemaSeed.COMPANY_ID))
                    .get().satisfies(cerrada -> assertThat(cerrada.getPeriod().to())
                            .isEqualTo(LocalDate.of(2026, 6, 30)));
            assertThat(port.findCurrentLine(SchemaSeed.COMPANY_ID, nucleo)).isEmpty();
        }

        @Test
        @DisplayName("una línea que no existe, o que es de otra empresa, se rechaza nombrando su id")
        void una_linea_ajena_o_inexistente_se_rechaza() {
            assertThatThrownBy(() -> port.closeLine(SchemaSeed.COMPANY_ID, -1L, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("-1");
            assertThatThrownBy(() -> port.closeLine(SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ITEM_ID, LocalDate.of(2026, 6, 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SchemaSeed.SUBSCRIPTION_ITEM_ID.toString());
        }
    }

    @Nested
    @DisplayName("Suceder una línea con el precio de hoy")
    class SucederLinea {

        @Test
        @DisplayName("cierra la anterior y abre la nueva PAID con succeeds_item_id y el precio dado")
        void cierra_la_anterior_y_abre_la_nueva_con_el_precio_dado() {
            LinePriceSnapshot precioDeHoy = new LinePriceSnapshot(nucleo, "CORE",
                    "Nucleo de prueba", "MODULE", 2, "TAXED", new BigDecimal("120000.00"),
                    new BigDecimal("19.00"));

            Long sucesoraId = port.succeedLine(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.SUBSCRIPTION_ITEM_ID, LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 30), precioDeHoy);
            entityManager.clear();

            assertThat(subscriptionItemRepository
                    .findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ITEM_ID, SchemaSeed.COMPANY_ID))
                    .get().satisfies(cerrada -> assertThat(cerrada.getPeriod().to())
                            .isEqualTo(LocalDate.of(2026, 6, 30)));
            assertThat(subscriptionItemRepository.findByIdAndCompanyId(sucesoraId,
                    SchemaSeed.COMPANY_ID)).get().satisfies(sucesora -> {
                        assertThat(sucesora.getSucceedsItemId())
                                .isEqualTo(SchemaSeed.SUBSCRIPTION_ITEM_ID);
                        assertThat(sucesora.getChargeMode()).isEqualTo("PAID");
                        assertThat(sucesora.getOrigin()).isEqualTo(ItemOrigin.ADDON);
                        assertThat(sucesora.getActivationPath()).isEqualTo("SELF_SERVICE");
                        assertThat(sucesora.getTrialEligibility()).isEqualTo("NEVER_FREE");
                        assertThat(sucesora.getMaxTrialDays()).isZero();
                        assertThat(sucesora.getTrialEndDate()).isNull();
                        assertThat(sucesora.getUnitAmount()).isEqualByComparingTo("120000.00");
                        assertThat(sucesora.getPeriod().from())
                                .isEqualTo(LocalDate.of(2026, 6, 30));
                        assertThat(sucesora.getPeriod().isOpen()).isTrue();
                    });
        }

        @Test
        @DisplayName("sin línea anterior, abre la línea nueva sola y sin succeeds_item_id")
        void sin_linea_anterior_abre_la_linea_nueva_sola() {
            insertarArticuloExtra(ARTICULO_EXTRA_ID, "EXTRA_MOD");
            LinePriceSnapshot precio = new LinePriceSnapshot(ARTICULO_EXTRA_ID, "EXTRA_MOD",
                    "Modulo extra", "MODULE", 0, "TAXED", new BigDecimal("30000.00"),
                    new BigDecimal("19.00"));

            Long nuevaId = port.succeedLine(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, null,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), precio);
            entityManager.clear();

            assertThat(
                    subscriptionItemRepository.findByIdAndCompanyId(nuevaId, SchemaSeed.COMPANY_ID))
                    .get().satisfies(nueva -> {
                        assertThat(nueva.getSucceedsItemId()).isNull();
                        assertThat(nueva.getOrigin()).isEqualTo(ItemOrigin.ADDON);
                        assertThat(nueva.getChargeMode()).isEqualTo("PAID");
                    });
        }

        @Test
        @DisplayName("compra a mitad de prueba: la TRIAL cierra en trial_end + 1 y la PAID nace con"
                + " el precio de hoy, no el congelado en la prueba")
        void compra_a_mitad_de_prueba_cierra_la_trial_y_abre_la_paid_con_el_precio_de_hoy() {
            insertarArticuloExtra(ARTICULO_TRIAL_ID, "TRIAL_MID");
            Long ventanaId = ventanaAbierta();
            concesion(ventanaId);
            Long lineaTrialId = lineaTrial();
            LocalDate cierre = FIN_PRUEBA.plusDays(1);
            LinePriceSnapshot precioDeHoy = new LinePriceSnapshot(ARTICULO_TRIAL_ID, "TRIAL_MID",
                    "Modulo en prueba", "MODULE", 0, "TAXED", new BigDecimal("95000.00"),
                    new BigDecimal("19.00"));

            Long sucesoraId = port.succeedLine(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    lineaTrialId, cierre, cierre, precioDeHoy);
            entityManager.clear();

            assertThat(subscriptionItemRepository.findByIdAndCompanyId(lineaTrialId,
                    SchemaSeed.COMPANY_ID)).get()
                    .satisfies(trialCerrada -> assertThat(trialCerrada.getPeriod().to())
                            .isEqualTo(FIN_PRUEBA.plusDays(1)));
            assertThat(subscriptionItemRepository.findByIdAndCompanyId(sucesoraId,
                    SchemaSeed.COMPANY_ID)).get().satisfies(paid -> {
                        assertThat(paid.getSucceedsItemId()).isEqualTo(lineaTrialId);
                        assertThat(paid.getChargeMode()).isEqualTo("PAID");
                        assertThat(paid.getOrigin()).isEqualTo(ItemOrigin.ADDON);
                        assertThat(paid.getActivationPath()).isEqualTo("SELF_SERVICE");
                        assertThat(paid.getTrialEndDate()).isNull();
                        // El precio es el de hoy (95000), no el congelado en la prueba (80000):
                        // la sucesion no arrastra la tarifa vieja.
                        assertThat(paid.getUnitAmount()).isEqualByComparingTo("95000.00");
                        assertThat(paid.getPeriod().from()).isEqualTo(cierre);
                    });
        }

        private Long lineaTrial() {
            CompanyJpaEntity company = companyJpaRepository.getReferenceById(SchemaSeed.COMPANY_ID);
            SubscriptionJpaEntity subscription = subscriptionJpaRepository
                    .getReferenceById(SchemaSeed.SUBSCRIPTION_ID);
            SubscriptionItem trial = SubscriptionItem.open(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, ARTICULO_TRIAL_ID, "TRIAL_MID", "Modulo en prueba",
                    SubscriptionItemType.MODULE, null, 1, null, 0, TaxTreatment.TAXED, 1,
                    new BigDecimal("80000.00"), BigDecimal.ZERO, BigDecimal.ZERO, false,
                    new BigDecimal("19.00"), EffectivePeriod.openFrom(CONCEDIDA_EL),
                    ItemOrigin.INITIAL, null, "TRIAL", "ELIGIBLE", DIAS_CONCEDIDOS, FIN_PRUEBA,
                    "SELF_SERVICE");
            return trialRepository.save(mapper.toJpa(trial, company, subscription)).getId();
        }
    }
}
