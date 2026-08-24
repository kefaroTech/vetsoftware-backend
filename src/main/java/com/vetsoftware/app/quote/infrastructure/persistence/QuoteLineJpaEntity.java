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
}
