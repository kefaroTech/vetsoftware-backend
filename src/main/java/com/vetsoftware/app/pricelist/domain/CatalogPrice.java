package com.vetsoftware.app.pricelist.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * El precio de un articulo dentro de una lista. Una misma cosa tiene varios
 * precios a la vez segun como se pague y cuanto se lleve.
 *
 * <p>
 * <strong>El impuesto vive aqui y no en el articulo.</strong> El catalogo de
 * impuestos que ya existe en el arbol es por clinica, asi que un
 * {@code catalog_items} global de plataforma no podia apuntar ahi. Cada precio
 * lleva su {@code taxRate} y su {@link TaxTreatment}, y esa pareja es la que se
 * congela en la cotizacion.
 *
 * <p>
 * La lista, el articulo y las fechas no se pueden reapuntar: mover un precio de
 * lista o de articulo no es editarlo, es crear otro. Por eso
 * {@code priceListId} y {@code catalogItemId} son {@code final}.
 */
public class CatalogPrice {

    private static final BigDecimal MAX_TAX_RATE = BigDecimal.valueOf(100);

    private final Long id;
    private final Long priceListId;
    private final Long catalogItemId;
    private BillingCycle billingCycle;
    private int tierMin;
    private Integer tierMax;
    private int includedQuantity;
    private BigDecimal unitAmount;
    private BigDecimal setupAmount;
    private BigDecimal taxRate;
    private TaxTreatment taxTreatment;
    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public CatalogPrice(Long id, Long priceListId, Long catalogItemId, BillingCycle billingCycle,
            int tierMin, Integer tierMax, int includedQuantity, BigDecimal unitAmount,
            BigDecimal setupAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
            LocalDateTime createdDate, Long version, boolean enabled) {
        validateScope(priceListId, catalogItemId, billingCycle);
        validateTier(tierMin, tierMax, includedQuantity);
        validateAmounts(unitAmount, setupAmount);
        validateTax(taxRate, taxTreatment);
        this.id = id;
        this.priceListId = priceListId;
        this.catalogItemId = catalogItemId;
        this.billingCycle = billingCycle;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.includedQuantity = includedQuantity;
        this.unitAmount = Money.scaled(unitAmount);
        this.setupAmount = Money.scaled(setupAmount);
        // La tarifa es DECIMAL(5,2) y comparte escala y redondeo con el dinero, asi que
        // se normaliza con el mismo helper en vez de con un setScale suelto: 19 y 19.00
        // tienen que ser el mismo dato al comparar contra chk_catalog_prices_tax_rate.
        this.taxRate = Money.scaled(taxRate);
        this.taxTreatment = taxTreatment;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static CatalogPrice create(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int tierMin, Integer tierMax, int includedQuantity,
            BigDecimal unitAmount, BigDecimal setupAmount, BigDecimal taxRate,
            TaxTreatment taxTreatment, LocalDateTime createdDate) {
        return new CatalogPrice(null, priceListId, catalogItemId, billingCycle, tierMin, tierMax,
                includedQuantity, unitAmount, setupAmount, taxRate, taxTreatment, createdDate, null,
                true);
    }

    public void update(BillingCycle billingCycle, int tierMin, Integer tierMax,
            int includedQuantity, BigDecimal unitAmount, BigDecimal setupAmount, BigDecimal taxRate,
            TaxTreatment taxTreatment) {
        validateScope(this.priceListId, this.catalogItemId, billingCycle);
        validateTier(tierMin, tierMax, includedQuantity);
        validateAmounts(unitAmount, setupAmount);
        validateTax(taxRate, taxTreatment);
        this.billingCycle = billingCycle;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.includedQuantity = includedQuantity;
        this.unitAmount = Money.scaled(unitAmount);
        this.setupAmount = Money.scaled(setupAmount);
        this.taxRate = Money.scaled(taxRate);
        this.taxTreatment = taxTreatment;
    }

    /**
     * Rechaza el candidato si su tramo pisa el de alguno de sus hermanos -los
     * precios del mismo {@code (lista, articulo, ciclo)}-.
     *
     * <p>
     * No es declarable en MySQL: no existen restricciones de exclusion, y
     * {@code uq_catalog_prices_tier} solo impide repetir el mismo {@code tier_min}.
     * La fila que se esta editando se excluye por identidad, no por posicion en la
     * lista, para que actualizar un tramo sin moverlo no choque consigo mismo.
     */
    public static void requireNoTierOverlap(CatalogPrice candidate, List<CatalogPrice> siblings) {
        siblings.stream().filter(sibling -> !candidate.isSameRowAs(sibling))
                .filter(sibling -> candidate.overlapsTier(sibling.tierMin, sibling.tierMax))
                .findFirst().ifPresent(sibling -> {
                    throw new CatalogPriceTierOverlapException(candidate.priceListId,
                            candidate.catalogItemId, candidate.billingCycle, candidate.tierMin,
                            candidate.tierMax, sibling.id);
                });
    }

    /**
     * Los tramos son cerrados por los dos lados y {@code tierMax} nulo significa
     * "del minimo en adelante", asi que el infinito se modela con
     * {@link Long#MAX_VALUE} y la comparacion se hace en {@code long} para que
     * {@code tierMax = Integer.MAX_VALUE} no se desborde.
     */
    public boolean overlapsTier(int otherMin, Integer otherMax) {
        long ownMax = tierMax == null ? Long.MAX_VALUE : tierMax;
        long theirMax = otherMax == null ? Long.MAX_VALUE : otherMax;
        return tierMin <= theirMax && otherMin <= ownMax;
    }

    private boolean isSameRowAs(CatalogPrice other) {
        return id != null && id.equals(other.id);
    }

    private static void validateScope(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle) {
        if (priceListId == null)
            throw new IllegalArgumentException("priceListId is required");
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (billingCycle == null)
            throw new IllegalArgumentException("billingCycle is required");
    }

    private static void validateTier(int tierMin, Integer tierMax, int includedQuantity) {
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be 1 or greater");
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException("tierMax must not be lower than tierMin");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
    }

    private static void validateAmounts(BigDecimal unitAmount, BigDecimal setupAmount) {
        if (unitAmount == null)
            throw new IllegalArgumentException("unitAmount is required");
        if (unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount cannot be negative");
        if (setupAmount == null)
            throw new IllegalArgumentException("setupAmount is required");
        if (setupAmount.signum() < 0)
            throw new IllegalArgumentException("setupAmount cannot be negative");
    }

    /**
     * Espejo de {@code chk_catalog_prices_tax_rate} y
     * {@code chk_catalog_prices_tax_coherence}.
     *
     * <p>
     * La coherencia no colapsa EXEMPT y EXCLUDED: los dos exigen tarifa cero y
     * siguen siendo codigos distintos, que es justo lo que la DIAN declara
     * distinto. Lo que impide es lo caro: un EXCLUDED con tarifa 19,00 produce un
     * IVA sobre una base que no debia llevarlo y sale en la declaracion bimestral.
     */
    private static void validateTax(BigDecimal taxRate, TaxTreatment taxTreatment) {
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        if (taxRate == null)
            throw new IllegalArgumentException("taxRate is required");
        if (taxRate.signum() < 0 || taxRate.compareTo(MAX_TAX_RATE) > 0)
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
        if (taxTreatment == TaxTreatment.TAXED && taxRate.signum() <= 0)
            throw new IllegalArgumentException("a TAXED catalog price requires a tax rate above 0");
        if (taxTreatment != TaxTreatment.TAXED && taxRate.signum() != 0)
            throw new IllegalArgumentException(
                    "a " + taxTreatment + " catalog price requires a tax rate of 0");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public int getTierMin() {
        return tierMin;
    }

    public Integer getTierMax() {
        return tierMax;
    }

    public int getIncludedQuantity() {
        return includedQuantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getSetupAmount() {
        return setupAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
