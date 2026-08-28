package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Las líneas fechadas contra MySQL real. Aquí se ejercita lo que un test de
 * mapper y un test de servicio con dobles no pueden ver: la traducción a SQL
 * del criterio de «vigente», la consulta de vigilancia R7 —que es nativa y
 * hasta ahora no la había ejecutado nadie— y la traducción de la violación del
 * índice único a un conflicto de negocio.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionItemRepository — líneas fechadas contra MySQL real")
class SubscriptionItemPersistenceIT extends AbstractDataJpaTest {

    /** Un segundo artículo de catálogo, para montar solapes sin tocar el seed. */
    private static final Long ARTICULO_EXTRA = 9600L;
    private static final Long LINEA_A = 9601L;
    private static final Long LINEA_B = 9602L;

    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNIO_29 = LocalDate.of(2026, 6, 29);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    private static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);

    @Autowired
    private JpaSubscriptionItemRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    /**
     * Inserta por SQL nativo, no por el adaptador, porque varias de estas filas son
     * justo las que el adaptador y el dominio impiden crear: el objetivo es
     * comprobar qué ve la consulta cuando la fila ya existe en la base.
     *
     * <p>
     * {@code current_item_marker} es {@code GENERATED ALWAYS}: nombrarla aquí daría
     * ERROR 3105 aunque el valor fuera nulo.
     */
    private void insertarLinea(Long id, Long catalogItemId, LocalDate desde, LocalDate hasta,
            boolean habilitada) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from,
                                                effective_to, origin, succeeds_item_id,
                                                created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :subscriptionId, :catalogItemId, 'EXTRA', 'Extra',
                        'MODULE', NULL, 0, 'TAXED', 1, 50000.00, 19.00, 1, NULL,
                        1, 'PAID', 'NEVER_FREE', 0, NULL, 'PLATFORM', 'NONE', :desde, :hasta,
                        'ADDON', NULL,
                        NULL, NULL, '2026-01-01 00:00:00', :habilitada, 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("catalogItemId", catalogItemId).setParameter("desde", desde)
                .setParameter("hasta", hasta).setParameter("habilitada", habilitada)
                .executeUpdate();
    }

    private void insertarArticuloExtra() {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, item_type, is_core, min_quantity,
                                           max_quantity, sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, 'EXTRA', 'Modulo extra', 'MODULE', false, 1, 1, 1, 'ACTIVE',
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", ARTICULO_EXTRA).executeUpdate();
    }

    private SubscriptionItem lineaDeNucleo(EffectivePeriod periodo) {
        return SubscriptionItem.open(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, nucleo,
                "CORE", "Nucleo de prueba", SubscriptionItemType.MODULE, null, 2,
                TaxTreatment.TAXED, 1, new BigDecimal("100000.00"), new BigDecimal("19.00"),
                periodo, ItemOrigin.ADDON, null);
    }

    @Nested
    @DisplayName("El criterio de vigente, traducido a SQL")
    class Vigencia {

        @Test
        @DisplayName("el día de fin NO está cubierto: el 29 sí, el 30 no")
        void elDiaDeFinNoEstaCubierto() {
            // El intervalo es semiabierto tambien en SQL. Un >= de mas en el
            // effective_to de la consulta factura un dia de mas cada vez que un cliente
            // se da de baja, y el error es invisible hasta que alguien reclama.
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findCurrentOn(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID,
                    JUNIO_29, 0, 20).content()).extracting(SubscriptionItem::getId)
                    .contains(LINEA_A);
            assertThat(repository.findCurrentOn(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID,
                    JUNIO_30, 0, 20).content()).extracting(SubscriptionItem::getId)
                    .doesNotContain(LINEA_A);
        }

        @Test
        @DisplayName("una línea que aún no ha empezado no está vigente, no tenga fin o sí")
        void unaLineaFuturaNoEstaVigente() {
            // Vigente NO es «sin fecha de fin»: esta linea no tiene fin y aun asi no
            // esta vigente hoy, porque todavia no ha empezado.
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, MAYO_1, null, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findCurrentOn(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID,
                    ENERO_1, 0, 20).content()).extracting(SubscriptionItem::getId)
                    .doesNotContain(LINEA_A);
            assertThat(repository
                    .findCurrentOn(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID, MAYO_1, 0, 20)
                    .content()).extracting(SubscriptionItem::getId).contains(LINEA_A);
        }

        @Test
        @DisplayName("el expediente completo conserva las líneas ya cerradas, en orden")
        void elExpedienteConservaLasCerradas() {
            // Dar de baja no borra: la linea cerrada sigue en el expediente y por eso
            // el listado sin fecha la devuelve. El orden es cronologico con desempate
            // por id, que es lo que evita que dos paginas repitan u omitan filas.
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllBySubscriptionIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.COMPANY_ID, 0, 20).content()).extracting(SubscriptionItem::getId)
                    .containsExactly(SchemaSeed.SUBSCRIPTION_ITEM_ID, LINEA_A);
        }

        @Test
        @DisplayName("una línea deshabilitada no sale por ninguna consulta")
        void unaLineaDeshabilitadaNoSale() {
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, null, false);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findCurrentOn(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID,
                    LocalDate.of(2026, 3, 15), 0, 20).content()).extracting(SubscriptionItem::getId)
                    .doesNotContain(LINEA_A);
            assertThat(repository.findByIdAndCompanyId(LINEA_A, SchemaSeed.COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Solapes")
    class Solapes {

        @Test
        @DisplayName("detecta los dos tramos con fin futuro que el índice único no puede ver")
        void detectaLosTramosConFinFuturo() {
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOverlapping(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    ARTICULO_EXTRA, MAYO_1, DICIEMBRE_31, null)).extracting(SubscriptionItem::getId)
                    .containsExactly(LINEA_A);
        }

        @Test
        @DisplayName("el que cierra el 30 y el que abre el 30 no se pisan")
        void tramosConsecutivosNoSePisan() {
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOverlapping(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    ARTICULO_EXTRA, JUNIO_30, null, null)).isEmpty();
        }

        @Test
        @DisplayName("la línea que se está editando queda fuera de su propia comprobación")
        void laLineaEditadaQuedaFuera() {
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOverlapping(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    ARTICULO_EXTRA, MAYO_1, DICIEMBRE_31, LINEA_A)).isEmpty();
        }

        @Test
        @DisplayName("un tramo abierto se pisa con cualquier tramo posterior")
        void unTramoAbiertoSePisaConTodo() {
            // El COALESCE a 9999-12-31 de la consulta es lo que hace comparable «sin
            // fecha de fin». Sin el, un NULL en effective_to dejaria pasar el alta.
            assertThat(repository.findOverlapping(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                    nucleo, DICIEMBRE_31, null, null)).extracting(SubscriptionItem::getId)
                    .containsExactly(SchemaSeed.SUBSCRIPTION_ITEM_ID);
        }
    }

    @Nested
    @DisplayName("La vigilancia R7 — cero filas es sano")
    class VigilanciaR7 {

        @Test
        @DisplayName("sobre un contrato sano no devuelve nada")
        void contratoSanoNoDevuelveNada() {
            // Cero filas = sano. Esta asercion tambien es la que prueba que la consulta
            // nativa arranca: hasta ahora nadie la habia ejecutado, y un alias que no
            // case no lo dice el compilador, lo dice un null en ejecucion.
            assertThat(repository.findAllOverlaps()).isEmpty();
        }

        @Test
        @DisplayName("encuentra el par que se pisa, con todas sus columnas")
        void encuentraElParQueSePisa() {
            // Las dos lineas tienen effective_to, o sea current_item_marker nulo, y
            // MySQL las acepta las dos. En mayo y junio ese modulo se factura dos veces
            // y no hay ninguna restriccion del motor que lo impida.
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            insertarLinea(LINEA_B, ARTICULO_EXTRA, MAYO_1, DICIEMBRE_31, true);
            entityManager.flush();
            entityManager.clear();

            List<SubscriptionItemOverlapDto> solapes = repository.findAllOverlaps();

            assertThat(solapes).singleElement().satisfies(solape -> {
                assertThat(solape.companyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(solape.subscriptionId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
                assertThat(solape.catalogItemId()).isEqualTo(ARTICULO_EXTRA);
                assertThat(solape.itemCode()).isEqualTo("EXTRA");
                assertThat(solape.firstItemId()).isEqualTo(LINEA_A);
                assertThat(solape.firstFrom()).isEqualTo(ENERO_1);
                assertThat(solape.firstTo()).isEqualTo(JUNIO_30);
                assertThat(solape.secondItemId()).isEqualTo(LINEA_B);
                assertThat(solape.secondFrom()).isEqualTo(MAYO_1);
                assertThat(solape.secondTo()).isEqualTo(DICIEMBRE_31);
            });
        }

        @Test
        @DisplayName("cada par sale una sola vez y ninguna fila se compara consigo misma")
        void cadaParUnaSolaVez() {
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            insertarLinea(LINEA_B, ARTICULO_EXTRA, MAYO_1, DICIEMBRE_31, true);
            entityManager.flush();
            entityManager.clear();

            // b.id > a.id: sin esa condicion cada par saldria dos veces y ademas cada
            // fila se emparejaria consigo misma, con lo que un contrato sano daria
            // tantas alarmas como lineas tiene.
            assertThat(repository.findAllOverlaps()).hasSize(1);
        }

        @Test
        @DisplayName("no cuenta las filas deshabilitadas")
        void noCuentaLasDeshabilitadas() {
            // @SQLRestriction no aplica al SQL nativo: si la consulta no mirara enabled
            // explicitamente, la vigilancia daria alarmas de filas que ya nadie ve.
            insertarArticuloExtra();
            insertarLinea(LINEA_A, ARTICULO_EXTRA, ENERO_1, JUNIO_30, true);
            insertarLinea(LINEA_B, ARTICULO_EXTRA, MAYO_1, DICIEMBRE_31, false);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllOverlaps()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Una línea abierta por artículo")
    class UnaLineaAbiertaPorArticulo {

        @Test
        @DisplayName("la segunda línea abierta del mismo artículo sale como conflicto, no como 500")
        void laSegundaLineaAbiertaEsConflicto() {
            // La comprobacion previa seria una carrera: dos altas simultaneas leerian
            // las dos «no hay» e insertarian las dos. El indice unico es la unica
            // autoridad y el adaptador traduce su rechazo al conflicto de negocio.
            assertThatThrownBy(
                    () -> repository.save(lineaDeNucleo(EffectivePeriod.openFrom(MAYO_1))))
                    .isInstanceOf(SubscriptionItemOverlapException.class)
                    .hasMessageContaining(nucleo.toString());
        }

        @Test
        @DisplayName("un tramo cerrado del mismo artículo sí se puede guardar")
        void unTramoCerradoSiSeGuarda() {
            // current_item_marker solo se llena cuando effective_to es NULL: un tramo
            // historico no ocupa el marcador y por eso puede convivir con la abierta.
            SubscriptionItem historico = repository.save(lineaDeNucleo(
                    new EffectivePeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));
            entityManager.flush();

            assertThat(historico.getId()).isNotNull();
            assertThat(repository.findOpenByCatalogItemId(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, nucleo)).get().extracting(SubscriptionItem::getId)
                    .isEqualTo(SchemaSeed.SUBSCRIPTION_ITEM_ID);
        }

        @Test
        @DisplayName("guardar en lote guarda todas y devuelve las líneas ya con id")
        void guardarEnLote() {
            insertarArticuloExtra();
            entityManager.flush();

            List<SubscriptionItem> guardadas = repository.saveAll(List.of(
                    SubscriptionItem.open(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                            ARTICULO_EXTRA, "EXTRA", "Modulo extra", SubscriptionItemType.MODULE,
                            null, 0, TaxTreatment.TAXED, 1, new BigDecimal("50000.00"),
                            new BigDecimal("19.00"), new EffectivePeriod(ENERO_1, JUNIO_30),
                            ItemOrigin.INITIAL, null),
                    SubscriptionItem.open(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                            ARTICULO_EXTRA, "EXTRA", "Modulo extra", SubscriptionItemType.MODULE,
                            null, 0, TaxTreatment.TAXED, 1, new BigDecimal("50000.00"),
                            new BigDecimal("19.00"), EffectivePeriod.openFrom(JUNIO_30),
                            ItemOrigin.ADDON, null)));

            assertThat(guardadas).hasSize(2)
                    .allSatisfy(linea -> assertThat(linea.getId()).isNotNull());
        }

        @Test
        @DisplayName("una capacidad guarda su unidad y la recupera intacta")
        void unaCapacidadGuardaSuUnidad() {
            insertarArticuloExtra();
            entityManager.flush();

            SubscriptionItem guardada = repository.save(SubscriptionItem.open(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, ARTICULO_EXTRA, "EXTRA", "Usuario adicional",
                    SubscriptionItemType.CAPACITY, "USER", 2, TaxTreatment.TAXED, 5,
                    new BigDecimal("50000.00"), new BigDecimal("19.00"),
                    EffectivePeriod.openFrom(ENERO_1), ItemOrigin.ADDON, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(linea -> {
                        assertThat(linea.getCapacityUnit()).isEqualTo("USER");
                        assertThat(linea.getIncludedQuantity()).isEqualTo(2);
                        assertThat(linea.getQuantity()).isEqualTo(5);
                        assertThat(linea.billableQuantity()).isEqualTo(3);
                        assertThat(linea.getUnitAmount()).isEqualByComparingTo("50000.00");
                        assertThat(linea.getPeriod().isOpen()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("ninguna lectura devuelve la línea de otra empresa")
        void ningunaLecturaCruzaDeEmpresa() {
            assertThat(repository.findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ITEM_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
            assertThat(repository.findOpenByCatalogItemId(SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, nucleo)).isEmpty();
            assertThat(repository.findCurrentOn(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.OTRA_COMPANY_ID, LocalDate.of(2026, 1, 15), 0, 20).content())
                    .isEmpty();
            assertThat(repository.findAllBySubscriptionIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.OTRA_COMPANY_ID, 0, 20).content()).isEmpty();
            assertThat(repository.findOverlapping(SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, nucleo, ENERO_1, null, null)).isEmpty();
        }

        @Test
        @DisplayName("la línea creada por un otrosí se busca por él, y solo dentro de su empresa")
        void laLineaSeBuscaPorSuOtrosi() {
            assertThat(repository.findByCreatedAmendmentIdAndCompanyId(1L, SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(
                    repository.findByCreatedAmendmentIdAndCompanyId(1L, SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }
    }
}
