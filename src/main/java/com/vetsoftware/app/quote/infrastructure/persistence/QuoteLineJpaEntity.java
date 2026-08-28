package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Renglon congelado de la oferta.
 *
 * <p>
 * <b>Sin {@code @Version}</b>, y esta exenta por escrito con el codigo
 * {@code E1_APPEND_ONLY}: se escribe con la cotizacion y ningun caso de uso la
 * reescribe; el bloqueo optimista vive en {@code quotes}, que si va versionada.
 * Por lo mismo no lleva {@code @SQLDelete}: no hay camino de baja de una linea
 * suelta.
 *
 * <p>
 * <b>Y tampoco lleva {@code @SQLRestriction("enabled = true")}</b>, que es una
 * decision y no un olvido. Si alguien desactivara una linea por SQL, ocultarla
 * al leer haria que la cabecera dejase de cuadrar con lo que el codigo ve y la
 * cotizacion se volveria ilegible; leyendolas todas, el documento sigue
 * mostrandose entero y el descuadre lo caza la consulta de vigilancia de R5,
 * que si filtra por {@code l.enabled = TRUE}. La aplicacion permanece legible;
 * la alerta suena donde tiene que sonar.
 *
 * <p>
 * {@code catalogItemId} es una columna plana a proposito: la FK esta en la base
 * para poder navegar, pero no hay asociacion en Java porque nada de esta linea
 * se vuelve a leer del catalogo.
 */
@Entity
@Table(name = "quote_lines")
public class QuoteLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 120)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private QuoteItemType itemType;

    /**
     * D-66 / R-QUOTE-09. El tramo que este renglon cubre, en unidades FACTURABLES.
     * Existe porque los tramos son acumulativos y una cantidad escalonada produce
     * varios renglones del mismo articulo a precios distintos: sin el tramo
     * escrito, la oferta ensena dos lineas iguales con importes distintos y no hay
     * forma de explicarlas. {@code tierMax} vacio es "de ahi en adelante".
     *
     * <p>
     * Valor inicial {@code 1} y {@code null}: el tramo unico y abierto, que es lo
     * que describe cualquier renglon de un articulo sin escalones.
     */
    @Column(name = "tier_min", nullable = false)
    private int tierMin = 1;

    @Column(name = "tier_max")
    private Integer tierMax;

    // [ANADIDO respecto de la ficha] contracted_quantity e included_quantity.
    // Sin ellas la linea solo ensena el resultado de la resta de R15 y explicar
    // por que se cobra 1 y no 3 obliga a volver a catalog_prices -es decir, a
    // releer la tarifa de hoy para justificar un documento de hace un ano, que es
    // justo lo que este modelo prohibe-. included_quantity se copia CONGELADA por
    // el mismo motivo que unit_amount: editar un tramo de la tarifa no puede
    // cambiar retroactivamente cuantas unidades le sobraban a quien firmo.
    @Column(name = "contracted_quantity", nullable = false)
    private int contractedQuantity;

    @Column(name = "included_quantity", nullable = false)
    private int includedQuantity;

    /** Unidades que se COBRAN: contratadas menos incluidas, con suelo en cero. */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    /**
     * D-86: un descuento CONDICIONADO -el de permanencia, por ejemplo- no reduce la
     * base del IVA, porque la norma solo excluye de la base los descuentos "no
     * sujetos a ninguna condición". La marca nace aquí, en el renglón de la oferta;
     * llevarla hasta la línea del contrato y el cargo es trabajo de las capas E y
     * M.
     *
     * <p>
     * <b>Ya no tiene valor inicial de hecho: lo escribe el dominio.</b> La marca
     * llega desde {@code QuoteLineCommand} con el porcentaje negociado —es la misma
     * negociacion— y {@code QuoteLine} bifurca con ella la base imponible. El
     * {@code false} de aqui es solo el estado de un objeto recien construido antes
     * de que el mapper lo rellene.
     */
    @Column(name = "discount_is_conditional", nullable = false)
    private boolean discountIsConditional = false;

    /**
     * Copia congelada de la política de prueba del catálogo el día de la oferta
     * (capa I). {@code maxTrialDays} es EL TOPE, y existe copiado porque una
     * restricción no puede mirar otra tabla: sin él, una oferta de 3.650 días de
     * prueba sería válida.
     *
     * <p>
     * Valores iniciales en el lado seguro -sin prueba- por el mismo motivo que en
     * {@code CatalogItemJpaEntity}: la cotización todavía no resuelve la política y
     * regalar por defecto es el error caro.
     */
    @Column(name = "trial_eligibility", nullable = false, length = 20)
    private String trialEligibility = "NEVER_FREE";

    @Column(name = "trial_outcome", length = 20)
    private String trialOutcome;

    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    @Column(name = "max_trial_days", nullable = false)
    private int maxTrialDays = 0;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected QuoteLineJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public QuoteItemType getItemType() {
        return itemType;
    }

    public void setItemType(QuoteItemType itemType) {
        this.itemType = itemType;
    }

    public int getTierMin() {
        return tierMin;
    }

    public void setTierMin(int tierMin) {
        this.tierMin = tierMin;
    }

    public Integer getTierMax() {
        return tierMax;
    }

    public void setTierMax(Integer tierMax) {
        this.tierMax = tierMax;
    }

    public int getContractedQuantity() {
        return contractedQuantity;
    }

    public void setContractedQuantity(int contractedQuantity) {
        this.contractedQuantity = contractedQuantity;
    }

    public int getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(int includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(BigDecimal unitAmount) {
        this.unitAmount = unitAmount;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public void setTaxTreatment(TaxTreatment taxTreatment) {
        this.taxTreatment = taxTreatment;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDiscountIsConditional() {
        return discountIsConditional;
    }

    public void setDiscountIsConditional(boolean discountIsConditional) {
        this.discountIsConditional = discountIsConditional;
    }

    public String getTrialEligibility() {
        return trialEligibility;
    }

    public void setTrialEligibility(String trialEligibility) {
        this.trialEligibility = trialEligibility;
    }

    public String getTrialOutcome() {
        return trialOutcome;
    }

    public void setTrialOutcome(String trialOutcome) {
        this.trialOutcome = trialOutcome;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public int getMaxTrialDays() {
        return maxTrialDays;
    }

    public void setMaxTrialDays(int maxTrialDays) {
        this.maxTrialDays = maxTrialDays;
    }
}
