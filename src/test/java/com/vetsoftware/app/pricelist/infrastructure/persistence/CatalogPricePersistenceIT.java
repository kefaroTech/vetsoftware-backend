package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogPriceRepository — tramos de precio contra MySQL real")
class CatalogPricePersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCatalogPriceRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("la consulta de tramo conserva ciclo, importes e impuesto")
    void la_consulta_de_tramo_conserva_importes_e_impuesto() {
        CatalogPrice guardado = repository.save(CatalogPrice.create(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL, 1, null, 2,
                new BigDecimal("950000.00"), BigDecimal.ZERO, new BigDecimal("19.00"),
                TaxTreatment.TAXED, CatalogPriceMother.CREADO_EL));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findTierScope(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL)).singleElement()
                .satisfies(precio -> {
                    assertThat(precio.getId()).isEqualTo(guardado.getId());
                    assertThat(precio.getUnitAmount()).isEqualByComparingTo("950000.00");
                    assertThat(precio.getTaxRate()).isEqualByComparingTo("19.00");
                    assertThat(precio.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
                });
        assertThat(repository.countByPriceListId(SchemaSeed.PRICE_LIST_ID)).isEqualTo(2L);
    }

    /**
     * El alcance de tramos es lo que alimenta a
     * {@code CatalogPrice.requireNoTierOverlap}. Si la consulta trajera precios de
     * otro ciclo de facturación, un tramo mensual [1,10] chocaría contra uno anual
     * [1,∞) y el alta se rechazaría sin motivo; si trajera de menos, dos tramos
     * solapados entrarían y el mismo artículo tendría dos precios válidos a la vez.
     */
    @Test
    @DisplayName("el alcance de tramos acota por lista, articulo y ciclo, los tres a la vez")
    void el_alcance_de_tramos_acota_por_los_tres_criterios() {
        repository
                .save(CatalogPrice.create(SchemaSeed.PRICE_LIST_ID, SchemaSeed.CATALOG_ITEM_CORE_ID,
                        BillingCycle.ANNUAL, 1, 10, 0, new BigDecimal("900000.00"), BigDecimal.ZERO,
                        new BigDecimal("19.00"), TaxTreatment.TAXED, CatalogPriceMother.CREADO_EL));
        entityManager.flush();
        entityManager.clear();

        // El seed ya dejo un MONTHLY [1, *) sobre el mismo articulo y la misma lista.
        assertThat(repository.findTierScope(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL)).singleElement()
                .satisfies(precio -> assertThat(precio.getBillingCycle())
                        .isEqualTo(BillingCycle.ANNUAL));
        assertThat(repository.findTierScope(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.MONTHLY)).singleElement()
                .satisfies(precio -> assertThat(precio.getBillingCycle())
                        .isEqualTo(BillingCycle.MONTHLY));
        assertThat(
                repository.findTierScope(SchemaSeed.PRICE_LIST_ID, 999_999L, BillingCycle.ANNUAL))
                .isEmpty();
    }

    /**
     * El {@code Sort} del listado nombra cuatro propiedades del modelo
     * (<em>catalogItemId</em>, <em>billingCycle</em>, <em>tierMin</em>,
     * <em>id</em>). Un nombre que no case con un atributo de la entidad no lo ve el
     * compilador: revienta en ejecución con un {@code PropertyReferenceException},
     * y solo cuando alguien abre el listado.
     */
    @Test
    @DisplayName("el listado por lista pagina con un orden total, con desempate por id")
    void el_listado_por_lista_pagina_con_orden_total() {
        repository.save(CatalogPrice.create(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL, 1, null, 0,
                new BigDecimal("900000.00"), BigDecimal.ZERO, new BigDecimal("19.00"),
                TaxTreatment.TAXED, CatalogPriceMother.CREADO_EL));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByPriceListId(SchemaSeed.PRICE_LIST_ID, 0, 20))
                .satisfies(pagina -> {
                    assertThat(pagina.content()).hasSize(2);
                    assertThat(pagina.totalElements()).isEqualTo(2L);
                    assertThat(pagina.page()).isZero();
                    assertThat(pagina.content())
                            .allSatisfy(precio -> assertThat(precio.getPriceListId())
                                    .isEqualTo(SchemaSeed.PRICE_LIST_ID));
                });
        assertThat(repository.findAllByPriceListId(999_999L, 0, 20).content()).isEmpty();
    }

    /**
     * {@code catalog_prices} está versionada, así que su {@code @SQLDelete} liga
     * dos parámetros. Si el {@code AND version = ?} faltara, el borrado
     * actualizaría cero filas <strong>sin lanzar nada</strong> y el precio seguiría
     * cotizando.
     */
    @Test
    @DisplayName("el borrado logico retira el precio del alcance de tramos y de la cuenta")
    void el_borrado_logico_retira_el_precio_del_alcance() {
        CatalogPrice guardado = repository.save(CatalogPrice.create(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL, 1, null, 0,
                new BigDecimal("900000.00"), BigDecimal.ZERO, new BigDecimal("19.00"),
                TaxTreatment.TAXED, CatalogPriceMother.CREADO_EL));
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.countByPriceListId(SchemaSeed.PRICE_LIST_ID)).isEqualTo(2L);

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(repository.findTierScope(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL)).isEmpty();
        assertThat(repository.countByPriceListId(SchemaSeed.PRICE_LIST_ID)).isEqualTo(1L);
    }

    /**
     * <strong>El mismo defecto que en {@code configurator}.</strong>
     * {@code uq_catalog_prices_tier} es
     * {@code (price_list_id, catalog_item_id, billing_cycle, tier_min)} y
     * <em>no</em> incluye {@code enabled}, así que un precio retirado sigue
     * ocupando su tramo. La única guardia del alta es
     * {@code CatalogPrice.requireNoTierOverlap}, que se alimenta de
     * {@code findTierScope} — una consulta derivada sobre una entidad con
     * {@code @SQLRestriction("enabled = true")} que no ve la fila retirada.
     *
     * <p>
     * Volver a poner el mismo tramo que se quitó pasa la comprobación de solape y
     * muere contra la clave única. Como {@code GlobalExceptionHandler} no mapea
     * {@code uq_catalog_prices_tier}, la respuesta es su 409 genérico
     * {@code DATA_INTEGRITY_VIOLATION}: quien está armando la tarifa no puede saber
     * que el tramo «sigue ahí», porque ninguna consulta se lo enseña. Las tres
     * tablas puente de {@code catalogitem} resuelven el mismo caso reactivando.
     */
    @Test
    @DisplayName("un precio retirado sigue ocupando su tramo, pero la guardia de solape no lo ve")
    void un_precio_retirado_sigue_ocupando_su_tramo_y_la_guardia_no_lo_ve() {
        CatalogPrice guardado = repository.save(CatalogPrice.create(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL, 1, null, 0,
                new BigDecimal("900000.00"), BigDecimal.ZERO, new BigDecimal("19.00"),
                TaxTreatment.TAXED, CatalogPriceMother.CREADO_EL));
        entityManager.flush();

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();

        // La guardia de solape cree que el tramo [1, *) quedo libre...
        assertThat(repository.findTierScope(SchemaSeed.PRICE_LIST_ID,
                SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.ANNUAL)).isEmpty();

        // ...y la fila sigue ahi, ocupando uq_catalog_prices_tier.
        Number filas = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM catalog_prices
                WHERE price_list_id = :lista AND catalog_item_id = :articulo
                  AND billing_cycle = 'ANNUAL' AND tier_min = 1
                """).setParameter("lista", SchemaSeed.PRICE_LIST_ID)
                .setParameter("articulo", SchemaSeed.CATALOG_ITEM_CORE_ID).getSingleResult();
        assertThat(filas.longValue()).isEqualTo(1L);
    }
}
