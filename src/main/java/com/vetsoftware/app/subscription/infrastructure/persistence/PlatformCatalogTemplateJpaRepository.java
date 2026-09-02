package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.platformbillingconfig.infrastructure.persistence.PlatformBillingConfigJpaEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Resuelve de una sola consulta el minimo estructural de la plataforma. Vive en
 * este slice —y no en los de catalogo o tarifa— porque el unico que necesita
 * esta pregunta es quien firma un contrato inicial.
 *
 * <p>
 * <strong>Es nativa y devuelve columnas crudas a proposito.</strong> Las
 * consultas derivadas equivalentes obligarian a nombrar en la firma los enums
 * de dominio de {@code pricelist} y {@code catalogitem} ({@code BillingCycle},
 * {@code CatalogItemStatus}, {@code TaxTreatment}), y el dominio de una feature
 * no se importa desde otra: la excepcion acotada del vertical slicing cubre
 * {@code XxxJpaEntity} y {@code XxxJpaRepository}, no los enums. Proyectando
 * {@code VARCHAR} el cruce se queda donde tiene que estar y la traduccion a los
 * enums propios la hace {@link JpaPlatformCatalogPort}.
 *
 * <p>
 * Los cinco {@code JOIN} son las cinco piezas del minimo, en el orden en que
 * las enumera {@code docs/db/suscripciones-modelo.md} §6.2: si falta
 * cualquiera, la consulta no devuelve fila y eso es exactamente la respuesta
 * que hace falta. {@code enabled = TRUE} va explicito porque
 * {@code @SQLRestriction} no aplica al SQL nativo.
 */
public interface PlatformCatalogTemplateJpaRepository
        extends
            JpaRepository<PlatformBillingConfigJpaEntity, Long> {

    @Query(value = """
            SELECT ci.id                  AS catalogItemId,
                   ci.code                AS itemCode,
                   ci.name                AS itemName,
                   ci.item_type           AS itemType,
                   ci.capacity_unit       AS capacityUnit,
                   ci.min_quantity        AS minQuantity,
                   pl.id                  AS priceListId,
                   cp.included_quantity   AS includedQuantity,
                   cp.unit_amount         AS unitAmount,
                   cp.tax_rate            AS taxRate,
                   cp.tax_treatment       AS taxTreatment,
                   cfg.default_grace_days AS defaultGraceDays,
                   cfg.default_trial_days AS defaultTrialDays
              FROM platform_billing_config cfg
              JOIN price_lists pl
                   ON  pl.id           = cfg.default_price_list_id
                   AND pl.status       = 'PUBLISHED'
                   AND pl.published_at IS NOT NULL
                   AND pl.enabled      = TRUE
              JOIN catalog_items ci
                   ON  ci.code               = 'CORE'
                   AND ci.structural_minimum = TRUE
                   AND ci.status             = 'ACTIVE'
                   AND ci.enabled            = TRUE
              JOIN catalog_prices cp
                   ON  cp.price_list_id   = pl.id
                   AND cp.catalog_item_id = ci.id
                   AND cp.billing_cycle   = :billingCycle
                   AND cp.tier_min        = 1
                   AND cp.enabled         = TRUE
              JOIN catalog_item_sub_modules cism
                   ON  cism.catalog_item_id = ci.id
                   AND cism.enabled         = TRUE
             WHERE cfg.singleton = 1
             LIMIT 1
            """, nativeQuery = true)
    Optional<InitialContractRow> findInitialContractTemplate(
            @Param("billingCycle") String billingCycle);

    /**
     * Las capacidades del minimo estructural: <strong>todos</strong> los
     * {@code catalog_items} de tipo {@code CAPACITY} marcados
     * {@code structural_minimum = TRUE} que tengan tramo publicado para el ciclo
     * pedido.
     *
     * <p>
     * <strong>Por que es una consulta aparte y no un {@code UNION} con la de
     * arriba.</strong> Aquella exige el {@code JOIN} a
     * {@code catalog_item_sub_modules}, y con razon: un modulo que no abre ninguna
     * pantalla no es un modulo. Una capacidad no abre ninguna pantalla por
     * definicion —es una cantidad, no un permiso—, asi que ese mismo {@code JOIN}
     * la dejaria fuera siempre. Meterlas en la misma consulta obligaria a un
     * {@code LEFT JOIN} que debilitaria la comprobacion del nucleo, que es
     * justamente la que no se puede debilitar.
     *
     * <p>
     * <strong>Aqui {@code structural_minimum} se usa como predicado de
     * conjunto</strong>, que es lo que la columna significa: «forma parte del
     * minimo estructural». La consulta del nucleo la usa junto a
     * {@code code = 'CORE'} y un {@code LIMIT 1} porque alli busca <em>el</em>
     * articulo que abre las pantallas base; esa confusion entre «el articulo CORE»
     * y «el conjunto de articulos del nucleo» es la que hacia nacer empresas sin
     * una sola capacidad (#490).
     *
     * <p>
     * Devuelve lista, posiblemente vacia. Quien decide si eso basta es el caso de
     * uso: el adaptador no sabe cuantas unidades hacen falta para operar.
     */
    @Query(value = """
            SELECT ci.id                  AS catalogItemId,
                   ci.code                AS itemCode,
                   ci.name                AS itemName,
                   ci.item_type           AS itemType,
                   ci.capacity_unit       AS capacityUnit,
                   ci.min_quantity        AS minQuantity,
                   pl.id                  AS priceListId,
                   cp.included_quantity   AS includedQuantity,
                   cp.unit_amount         AS unitAmount,
                   cp.tax_rate            AS taxRate,
                   cp.tax_treatment       AS taxTreatment,
                   cfg.default_grace_days AS defaultGraceDays,
                   cfg.default_trial_days AS defaultTrialDays
              FROM platform_billing_config cfg
              JOIN price_lists pl
                   ON  pl.id           = cfg.default_price_list_id
                   AND pl.status       = 'PUBLISHED'
                   AND pl.published_at IS NOT NULL
                   AND pl.enabled      = TRUE
              JOIN catalog_items ci
                   ON  ci.structural_minimum = TRUE
                   AND ci.item_type          = 'CAPACITY'
                   AND ci.capacity_unit      IS NOT NULL
                   AND ci.status             = 'ACTIVE'
                   AND ci.enabled            = TRUE
              JOIN catalog_prices cp
                   ON  cp.price_list_id   = pl.id
                   AND cp.catalog_item_id = ci.id
                   AND cp.billing_cycle   = :billingCycle
                   AND cp.tier_min        = 1
                   AND cp.enabled         = TRUE
             WHERE cfg.singleton = 1
             ORDER BY ci.sort_order, ci.id
            """, nativeQuery = true)
    List<InitialContractRow> findInitialCapacityTemplates(
            @Param("billingCycle") String billingCycle);

    /**
     * Solo los dias de gracia por defecto. Es una consulta aparte y no una
     * proyeccion de la de arriba porque aquella exige las piezas del minimo
     * estructural: un alta por API que no necesita el nucleo tampoco tiene por que
     * quedarse sin el valor por defecto de la plataforma.
     */
    @Query(value = """
            SELECT cfg.default_grace_days
              FROM platform_billing_config cfg
             WHERE cfg.singleton = 1
             LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findDefaultGraceDays();

    /** Proyeccion cruda: nada aqui conoce el dominio de otra feature. */
    interface InitialContractRow {
        Long getCatalogItemId();

        String getItemCode();

        String getItemName();

        String getItemType();

        String getCapacityUnit();

        Integer getMinQuantity();

        Long getPriceListId();

        Integer getIncludedQuantity();

        BigDecimal getUnitAmount();

        BigDecimal getTaxRate();

        String getTaxTreatment();

        Integer getDefaultGraceDays();

        Integer getDefaultTrialDays();
    }
}
