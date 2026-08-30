package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
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
 * La consulta que decide qué líneas del contrato devengan un día, contra MySQL
 * real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta.</b> El adaptador se llama {@code ...Port}
 * y no {@code Jpa<Algo>Repository}, así que {@code ADAPTADOR_JPA_CON_RODAJA} no
 * lo alcanza: quedaba cubierto solo por mocks de Mockito en el caso de uso, que
 * es tanto como decir que nadie ejecutaba nunca su SQL. Es SQL nativo con
 * <b>acceso posicional</b> ({@code fila[0]}…{@code fila[12]}) y trece columnas:
 * reordenar el {@code SELECT} o renombrar una columna en una migración no lo ve
 * el compilador.
 *
 * <p>
 * <b>Lo que decide.</b> Dinero. Una línea de más en la lista es una línea
 * cobrada de más al cliente; una de menos, ingreso perdido. El caso central es
 * el del <b>día del relevo</b>: el propio javadoc del adaptador avisa de que
 * con el extremo cerrado se cobrarían las dos líneas ese día, y hasta ahora ese
 * aviso no lo comprobaba nadie.
 *
 * <p>
 * <b>Cómo está montado el escenario para que un SQL equivocado se vea.</b> La
 * saliente y la entrante comparten artículo y se relevan exactamente en
 * {@link #DIA_DEL_RELEVO}, así que cualquier extremo cerrado devuelve las dos;
 * hay una línea de otra clínica y otra de otro contrato de la misma clínica,
 * así que perder cualquiera de las dos cláusulas de acotación se caza; y los
 * cuatro identificadores de la fila viven en rangos disjuntos (72xx, 900, 970,
 * 723x) para que cruzar dos columnas no pueda acertar por casualidad.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBillableSubscriptionItemPort — la vigencia semiabierta contra MySQL real")
class BillableSubscriptionItemPortIT extends AbstractDataJpaTest {

    /**
     * El día en que una línea se cierra y su sucesora se abre. Pertenece a la
     * sucesora, y solo a ella.
     */
    private static final LocalDate DIA_DEL_RELEVO = LocalDate.of(2026, 3, 15);
    private static final LocalDate VISPERA_DEL_RELEVO = LocalDate.of(2026, 3, 14);

    /** Contrato cancelado de la MISMA clínica: prueba el filtro por contrato. */
    private static final Long SUSCRIPCION_CANCELADA = 7205L;

    private static final Long LINEA_VIGENTE = 7210L;
    /** Se cierra el día del relevo. Ese día ya NO es suya. */
    private static final Long LINEA_SALIENTE = 7211L;
    /** Se abre el día del relevo. Ese día es suya, y solo suya. */
    private static final Long LINEA_ENTRANTE = 7212L;
    private static final Long LINEA_FUTURA = 7213L;
    /**
     * Baja lógica ({@code enabled = FALSE}) y por lo demás perfectamente vigente.
     */
    private static final Long LINEA_DE_BAJA = 7214L;
    /** Gratis con tope, y con su tarifa real guardada (R-TRIAL-14). */
    private static final Long LINEA_GRATIS_CON_TOPE = 7215L;
    private static final Long LINEA_VENCIDA = 7216L;
    private static final Long LINEA_AJENA = 7217L;
    private static final Long LINEA_DE_OTRO_CONTRATO = 7218L;

    private static final Long ART_VIGENTE = 7230L;
    private static final Long ART_RELEVO = 7231L;
    private static final Long ART_FUTURO = 7232L;
    private static final Long ART_DE_BAJA = 7233L;
    private static final Long ART_GRATIS = 7234L;
    private static final Long ART_VENCIDO = 7235L;

    /**
     * Los tres decimales de la fila vigente, deliberadamente distintos entre sí y
     * de cualquier otro número del escenario. Si {@code unit_amount} y
     * {@code tax_rate} cambiaran de sitio en el {@code SELECT}, con dos valores
     * parecidos no se notaría.
     */
    private static final BigDecimal IMPORTE_VIGENTE = new BigDecimal("11111.11");
    private static final BigDecimal TASA_VIGENTE = new BigDecimal("19.00");
    private static final BigDecimal IMPORTE_GRATIS = new BigDecimal("77777.77");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JpaBillableSubscriptionItemPort port;

    @BeforeEach
    void sembrarElContrato() {
        SchemaSeed.seed(entityManager);

        articulo(ART_VIGENTE, "TEST_BILL_VIGENTE", "Vigente");
        articulo(ART_RELEVO, "TEST_BILL_RELEVO", "Relevo");
        articulo(ART_FUTURO, "TEST_BILL_FUTURO", "Futuro");
        articulo(ART_DE_BAJA, "TEST_BILL_DE_BAJA", "De baja");
        articulo(ART_GRATIS, "TEST_BILL_GRATIS", "Gratis con tope");
        articulo(ART_VENCIDO, "TEST_BILL_VENCIDO", "Vencido");

        contratoCancelado();

        linea(LINEA_VIGENTE, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_VIGENTE,
                "Vigente al relevo", "PAID", 7, 3, IMPORTE_VIGENTE, TASA_VIGENTE, "TAXED",
                "2026-03-01", null, true);
        // Las dos mitades del relevo. Mismo articulo a proposito: si la saliente y la
        // entrante fueran articulos distintos, un extremo cerrado seguiria devolviendo
        // dos filas pero el error se leeria como «dos productos», no como «el mismo
        // producto cobrado dos veces», que es lo que de verdad pasa en la factura.
        linea(LINEA_SALIENTE, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_RELEVO,
                "Saliente", "PAID", 2, 0, new BigDecimal("22222.22"), new BigDecimal("0.00"),
                "EXEMPT", "2026-01-01", "2026-03-15", true);
        linea(LINEA_ENTRANTE, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_RELEVO,
                "Entrante", "PAID", 4, 1, new BigDecimal("33333.33"), new BigDecimal("0.00"),
                "EXCLUDED", "2026-03-15", null, true);
        linea(LINEA_FUTURA, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_FUTURO, "Futura",
                "PAID", 1, 0, new BigDecimal("44444.44"), TASA_VIGENTE, "TAXED", "2026-06-01", null,
                true);
        linea(LINEA_DE_BAJA, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_DE_BAJA,
                "De baja", "PAID", 1, 0, new BigDecimal("55555.55"), TASA_VIGENTE, "TAXED",
                "2026-03-01", null, false);
        lineaGratisConTope(LINEA_GRATIS_CON_TOPE, ART_GRATIS);
        lineaVencida(LINEA_VENCIDA, ART_VENCIDO);

        linea(LINEA_AJENA, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID, ART_VIGENTE,
                "Ajena", "PAID", 9, 0, new BigDecimal("66666.66"), TASA_VIGENTE, "TAXED",
                "2026-03-01", null, true);
        linea(LINEA_DE_OTRO_CONTRATO, SchemaSeed.COMPANY_ID, SUSCRIPCION_CANCELADA, ART_VIGENTE,
                "De otro contrato", "PAID", 6, 0, new BigDecimal("88888.88"), TASA_VIGENTE, "TAXED",
                "2026-03-01", null, true);

        entityManager.flush();
    }

    private java.util.List<BillableSubscriptionItem> vigentesElDiaDelRelevo() {
        return port.findCurrentOn(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                DIA_DEL_RELEVO);
    }

    @Nested
    @DisplayName("Vigencia semiabierta")
    class VigenciaSemiabierta {

        /**
         * <b>El caso que justifica esta rodaja entera.</b> Con
         * {@code effective_to >= :day} en vez de {@code >}, la saliente y la entrante
         * salen las dos y el cliente paga el mismo producto dos veces el día del
         * relevo. Se afirman las dos caras —que la entrante está y que la saliente no—
         * porque cada una caza una mutación distinta del extremo.
         */
        @Test
        @DisplayName("el día del relevo devenga la sucesora, y solo la sucesora")
        void el_dia_del_relevo_devenga_solo_la_sucesora() {
            // Precondicion: las dos filas existen y se relevan exactamente ese dia. Sin
            // esto, un fixture que perdiera la saliente dejaria el test en verde sin
            // haber probado nunca el extremo.
            assertThat(filasQueSeCierranEl(DIA_DEL_RELEVO)).isEqualTo(1);

            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .contains(LINEA_ENTRANTE).doesNotContain(LINEA_SALIENTE);
        }

        @Test
        @DisplayName("la víspera del relevo devenga la saliente, y no la sucesora")
        void la_vispera_del_relevo_devenga_la_saliente() {
            assertThat(port.findCurrentOn(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    VISPERA_DEL_RELEVO)).extracting(BillableSubscriptionItem::id)
                    .contains(LINEA_SALIENTE).doesNotContain(LINEA_ENTRANTE);
        }

        @Test
        @DisplayName("una línea que empieza más tarde no devenga hoy")
        void una_linea_que_empieza_mas_tarde_no_devenga_hoy() {
            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .doesNotContain(LINEA_FUTURA);
        }

        @Test
        @DisplayName("una línea sin fecha de cierre sigue devengando")
        void una_linea_sin_fecha_de_cierre_sigue_devengando() {
            assertThat(vigentesElDiaDelRelevo())
                    .filteredOn(linea -> LINEA_VIGENTE.equals(linea.id())).singleElement()
                    .extracting(BillableSubscriptionItem::effectiveTo).isNull();
        }

        @Test
        @DisplayName("las líneas llegan ordenadas por id, que es lo que fija el orden de la factura")
        void las_lineas_llegan_ordenadas_por_id() {
            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .isSorted();
        }
    }

    @Nested
    @DisplayName("Mapeo posicional")
    class MapeoPosicional {

        @Test
        @DisplayName("cada columna del SELECT cae en el campo que dice, las trece")
        void cada_columna_cae_en_el_campo_que_dice() {
            // La comprobacion de fondo: el mapeo es POSICIONAL. Todos los valores de la
            // fila son distintos entre si —dos Long de contrato, dos enteros, dos
            // decimales—, asi que intercambiar cualquier par de columnas rompe este caso.
            assertThat(vigentesElDiaDelRelevo())
                    .filteredOn(linea -> LINEA_VIGENTE.equals(linea.id())).singleElement()
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(new BillableSubscriptionItem(LINEA_VIGENTE, SchemaSeed.COMPANY_ID,
                            SchemaSeed.SUBSCRIPTION_ID, ART_VIGENTE, "Vigente al relevo",
                            ItemChargeMode.PAID, 7, 3, IMPORTE_VIGENTE, TASA_VIGENTE,
                            TaxTreatment.TAXED, LocalDate.of(2026, 3, 1), null));
        }

        @Test
        @DisplayName("las dos fechas no se cruzan: apertura y cierre caen cada una en su campo")
        void las_dos_fechas_no_se_cruzan() {
            assertThat(port.findCurrentOn(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    VISPERA_DEL_RELEVO)).filteredOn(linea -> LINEA_SALIENTE.equals(linea.id()))
                    .singleElement().satisfies(linea -> {
                        assertThat(linea.effectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
                        assertThat(linea.effectiveTo()).isEqualTo(DIA_DEL_RELEVO);
                    });
        }
    }

    @Nested
    @DisplayName("Modo de cobro")
    class ModoDeCobro {

        /**
         * El {@code charge_mode} se proyecta, no se filtra: quien decide si devenga es
         * {@link BillableSubscriptionItem#devenga}, que se puede probar sin base de
         * datos. Este caso comprueba que la consulta <b>no</b> se adelanta a esa
         * decisión.
         */
        @Test
        @DisplayName("una línea gratis con tope llega, con su modo y su tarifa real")
        void una_linea_gratis_con_tope_llega_con_su_tarifa_real() {
            assertThat(vigentesElDiaDelRelevo())
                    .filteredOn(linea -> LINEA_GRATIS_CON_TOPE.equals(linea.id())).singleElement()
                    .satisfies(linea -> {
                        assertThat(linea.chargeMode()).isEqualTo(ItemChargeMode.FREE_LIMITED);
                        // El precio completo, no cero: R-TRIAL-14. Si la consulta trajera ceros,
                        // el dia que la prueba termina el cliente empezaria a pagar nada.
                        assertThat(linea.unitAmount()).isEqualByComparingTo(IMPORTE_GRATIS);
                        assertThat(linea.devenga(DIA_DEL_RELEVO)).isFalse();
                    });
        }

        @Test
        @DisplayName("una línea vencida en solo lectura también llega, y no devenga")
        void una_linea_vencida_tambien_llega_y_no_devenga() {
            assertThat(vigentesElDiaDelRelevo())
                    .filteredOn(linea -> LINEA_VENCIDA.equals(linea.id())).singleElement()
                    .satisfies(linea -> {
                        assertThat(linea.chargeMode()).isEqualTo(ItemChargeMode.EXPIRED_READ_ONLY);
                        assertThat(linea.devenga(DIA_DEL_RELEVO)).isFalse();
                    });
        }

        @Test
        @DisplayName("solo la línea PAID devenga de verdad")
        void solo_la_linea_paid_devenga_de_verdad() {
            assertThat(vigentesElDiaDelRelevo()).filteredOn(linea -> linea.devenga(DIA_DEL_RELEVO))
                    .extracting(BillableSubscriptionItem::chargeMode)
                    .containsOnly(ItemChargeMode.PAID);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la línea de otra clínica no entra en esta factura")
        void la_linea_de_otra_clinica_no_entra_en_esta_factura() {
            // Existe y es vigente ese dia: si no se comprobara, el caso pasaria por
            // ausencia de fila y no por la clausula de empresa.
            assertThat(filasVigentesConId(LINEA_AJENA)).isEqualTo(1);

            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .doesNotContain(LINEA_AJENA);
        }

        @Test
        @DisplayName("la línea de otro contrato de la misma clínica tampoco")
        void la_linea_de_otro_contrato_de_la_misma_clinica_tampoco() {
            assertThat(filasVigentesConId(LINEA_DE_OTRO_CONTRATO)).isEqualTo(1);

            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .doesNotContain(LINEA_DE_OTRO_CONTRATO);
        }

        @Test
        @DisplayName("todo lo devuelto pertenece a la clínica y al contrato pedidos")
        void todo_lo_devuelto_pertenece_a_la_clinica_y_al_contrato_pedidos() {
            assertThat(vigentesElDiaDelRelevo()).isNotEmpty().allSatisfy(linea -> {
                assertThat(linea.companyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(linea.subscriptionId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            });
        }

        @Test
        @DisplayName("sin empresa, sin contrato o sin día no se consulta nada")
        void sin_empresa_sin_contrato_o_sin_dia_no_se_consulta_nada() {
            assertThat(port.findCurrentOn(null, SchemaSeed.SUBSCRIPTION_ID, DIA_DEL_RELEVO))
                    .isEmpty();
            assertThat(port.findCurrentOn(SchemaSeed.COMPANY_ID, null, DIA_DEL_RELEVO)).isEmpty();
            assertThat(port.findCurrentOn(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, null))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Baja lógica")
    class BajaLogica {

        /**
         * <b>Este caso fija la coherencia entre las tres consultas de
         * {@code subscription_items}.</b>
         *
         * <p>
         * {@code SELECT_VIGENTES} filtra ahora {@code i.enabled = TRUE}, igual que
         * {@code entitlement/ContractItemJpaRepository#findModuleLines} y
         * {@code companylimitoverride/EffectiveLimitCandidateJpaRepository#findFreeTierCeilings},
         * que son las otras dos lecturas de la misma tabla para el mismo contrato.
         *
         * <p>
         * <b>Lo que este caso impide que vuelva.</b> Sin el filtro, una línea dada de
         * baja lógica dejaba de conceder permisos y dejaba de conceder cupo —las dos
         * hermanas sí lo miraban— y <b>seguía devengando</b>: al cliente se le retiraba
         * lo contratado y se le seguía cobrando, sin que nada en el sistema dijera que
         * las tres consultas habían dejado de hablar de la misma población. El defecto
         * solo se veía en la factura del mes siguiente.
         */
        @Test
        @DisplayName("una línea dada de baja lógica deja de devengar, como deja de conceder")
        void una_linea_dada_de_baja_logica_deja_de_devengar() {
            assertThat(estaDeBaja(LINEA_DE_BAJA)).isTrue();

            assertThat(vigentesElDiaDelRelevo()).extracting(BillableSubscriptionItem::id)
                    .doesNotContain(LINEA_DE_BAJA);
        }
    }

    private long filasQueSeCierranEl(LocalDate dia) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM subscription_items
                 WHERE subscription_id = :contrato AND effective_to = :dia
                """).setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID).setParameter("dia", dia)
                .getSingleResult()).longValue();
    }

    private long filasVigentesConId(Long id) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM subscription_items
                 WHERE id = :id AND effective_from <= :dia
                   AND (effective_to IS NULL OR effective_to > :dia)
                """).setParameter("id", id).setParameter("dia", DIA_DEL_RELEVO).getSingleResult())
                .longValue();
    }

    private boolean estaDeBaja(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT enabled FROM subscription_items WHERE id = :id")
                .setParameter("id", id).getSingleResult()).intValue() == 0;
    }

    private void articulo(Long id, String code, String name) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, is_core,
                                           min_quantity, max_quantity, sort_order, status,
                                           trial_eligibility, default_trial_days, trial_outcome,
                                           service_nature, created_date, enabled, version)
                VALUES (:id, :code, :name, 'MODULE', NULL, false, 1, NULL, 0, 'ACTIVE',
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .executeUpdate();
    }

    /**
     * Segundo contrato de la MISMA clínica, y por eso {@code CANCELLED}:
     * {@code uq_subscriptions_active_company} sobre {@code active_marker} impone un
     * solo contrato vivo por empresa, y un segundo {@code ACTIVE} reventaría el
     * fixture entero.
     */
    private void contratoCancelado() {
        entityManager.createNativeQuery("""
                INSERT INTO subscriptions (id, subscription_number, company_id, quote_id,
                                           price_list_id, billing_cycle, status, start_date,
                                           trial_end_date, current_period_start,
                                           current_period_end, next_billing_date,
                                           commitment_end_date, grace_days, past_due_since,
                                           auto_renew, created_date, enabled, version)
                VALUES (:id, 'SUS-TEST-007205', :companyId, NULL, :lista, 'MONTHLY', 'CANCELLED',
                        '2026-01-01', NULL, '2026-01-01', '2026-01-31', '2026-02-01', NULL, 5,
                        NULL, false, '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", SUSCRIPCION_CANCELADA)
                .setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("lista", SchemaSeed.PRICE_LIST_ID).executeUpdate();
    }

    /**
     * <b>No es {@code TRIAL} a propósito.</b> Una línea {@code TRIAL} exige
     * {@code trial_end_date}, y esa columna arrastra el FK compuesto
     * {@code fk_subscription_items_trial_grant} sobre
     * {@code (company_id, catalog_item_id, trial_end_date)}: habría que sembrar
     * {@code company_trial_grants} y {@code trial_windows} enteras para probar una
     * propiedad del SQL -que el modo se proyecta y la tarifa no se pone a cero- que
     * {@code FREE_LIMITED} demuestra igual. {@code chk_..._never_free_is_paid}
     * obliga a que cualquier modo distinto de {@code PAID} sea {@code ELIGIBLE}, y
     * {@code chk_..._max_trial_days} a que entonces {@code max_trial_days} sea > 0.
     */
    private void lineaGratisConTope(Long id, Long catalogItemId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from, effective_to,
                                                origin, succeeds_item_id, created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :contrato, :articulo, 'TEST_BILL_GRATIS', 'Gratis',
                        'MODULE', NULL, 0, 'TAXED', 1, :importe, 19.00, 1, NULL,
                        1, 'FREE_LIMITED', 'ELIGIBLE', 30, NULL, 'SELF_SERVICE',
                        'NONE', '2026-03-01', NULL, 'INITIAL', NULL, NULL, NULL,
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("articulo", catalogItemId).setParameter("importe", IMPORTE_GRATIS)
                .executeUpdate();
    }

    private void lineaVencida(Long id, Long catalogItemId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from, effective_to,
                                                origin, succeeds_item_id, created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :contrato, :articulo, 'TEST_BILL_VENCIDO', 'Vencida',
                        'MODULE', NULL, 0, 'TAXED', 1, 99999.99, 19.00, 1, NULL,
                        1, 'EXPIRED_READ_ONLY', 'ELIGIBLE', 15, NULL, 'SELF_SERVICE',
                        'NONE', '2026-03-01', NULL, 'INITIAL', NULL, NULL, NULL,
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("articulo", catalogItemId).executeUpdate();
    }

    private void linea(Long id, Long companyId, Long subscriptionId, Long catalogItemId,
            String itemName, String chargeMode, int quantity, int included, BigDecimal unitAmount,
            BigDecimal taxRate, String taxTreatment, String desde, String hasta, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from, effective_to,
                                                origin, succeeds_item_id, created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :contrato, :articulo, 'TEST_BILL', :itemName,
                        'MODULE', NULL, :incluidas, :tratamiento, :cantidad, :importe, :tasa,
                        1, NULL, 1, :modo, 'NEVER_FREE', 0, NULL, 'PLATFORM',
                        'NONE', :desde, :hasta, 'INITIAL', NULL, NULL, NULL,
                        '2026-01-01 00:00:00', :enabled, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("contrato", subscriptionId).setParameter("articulo", catalogItemId)
                .setParameter("itemName", itemName).setParameter("incluidas", included)
                .setParameter("tratamiento", taxTreatment).setParameter("cantidad", quantity)
                .setParameter("importe", unitAmount).setParameter("tasa", taxRate)
                .setParameter("modo", chargeMode).setParameter("desde", LocalDate.parse(desde))
                .setParameter("hasta", hasta == null ? null : LocalDate.parse(hasta))
                .setParameter("enabled", enabled).executeUpdate();
    }
}
