package com.vetsoftware.app.quote.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El renglon de la oferta, con los datos del articulo CONGELADOS.
 *
 * <p>
 * {@code itemCode}, {@code itemName}, {@code itemType}, {@code unitAmount},
 * {@code taxRate} y {@code taxTreatment} son COPIAS, no referencias. El
 * {@code catalogItemId} existe solo para poder navegar; releer el catalogo por
 * el para pintar esta linea rompe el modelo entero, porque manana el articulo
 * puede llamarse distinto, costar distinto o haberse retirado, y la cotizacion
 * tiene que seguir diciendo lo que el cliente leyo.
 *
 * <p>
 * <b>La aritmetica se recomputa en el constructor</b>, tanto al crear como al
 * leer de la base. Es barato y es lo unico que convierte los cinco importes de
 * una fila en una prueba, en vez de en cinco numeros sueltos que pueden
 * contradecirse.
 */
public class QuoteLine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_CODE = 50;
    private static final int MAX_NAME = 120;

    private final Long id;
    private final int lineNumber;
    private final Long catalogItemId;
    private final String itemCode;
    private final String itemName;
    private final QuoteItemType itemType;
    private final int contractedQuantity;
    private final int includedQuantity;
    private final int quantity;
    private final BigDecimal unitAmount;
    private final BigDecimal discountPercent;
    private final BigDecimal discountAmount;
    private final BigDecimal taxRate;
    private final TaxTreatment taxTreatment;
    private final BigDecimal taxAmount;
    private final BigDecimal lineTotal;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public QuoteLine(Long id, int lineNumber, Long catalogItemId, String itemCode, String itemName,
            QuoteItemType itemType, int contractedQuantity, int includedQuantity, int quantity,
            BigDecimal unitAmount, BigDecimal discountPercent, BigDecimal discountAmount,
            BigDecimal taxRate, TaxTreatment taxTreatment, BigDecimal taxAmount,
            BigDecimal lineTotal, LocalDateTime createdDate, boolean enabled) {
        validateIdentity(lineNumber, catalogItemId, itemCode, itemName, itemType, quantity);
        validateQuantities(itemType, contractedQuantity, includedQuantity, quantity);
        validateAmounts(unitAmount, discountPercent, discountAmount, taxRate, taxTreatment,
                taxAmount, lineTotal);
        this.id = id;
        this.lineNumber = lineNumber;
        this.catalogItemId = catalogItemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.contractedQuantity = contractedQuantity;
        this.includedQuantity = includedQuantity;
        this.quantity = quantity;
        this.unitAmount = Money.scaled(unitAmount);
        this.discountPercent = discountPercent.setScale(2, Money.ROUND);
        this.discountAmount = Money.scaled(discountAmount);
        this.taxRate = taxRate.setScale(2, Money.ROUND);
        this.taxTreatment = taxTreatment;
        this.taxAmount = Money.scaled(taxAmount);
        this.lineTotal = Money.scaled(lineTotal);
        this.createdDate = createdDate;
        this.enabled = enabled;
        verifyArithmetic();
    }

    /**
     * Congela una linea nueva a partir del articulo y del precio leidos del
     * catalogo. Es el UNICO sitio donde se calculan los cuatro importes de la
     * linea; a partir de aqui son datos y no se recalculan nunca mas.
     *
     * @param discountPercent
     *            descuento negociado en porcentaje. El importe en pesos se deriva
     *            de el: guardar los dos es lo que permite saber que se negocio y
     *            cuanto costo, sin que puedan contradecirse.
     */
    public static QuoteLine freeze(int lineNumber, CatalogItemRef item, CatalogPriceRef price,
            int contractedQuantity, BigDecimal discountPercent, LocalDateTime createdDate) {
        if (item == null)
            throw new IllegalArgumentException("catalog item is required");
        if (price == null)
            throw new IllegalArgumentException("catalog price is required");
        int included = price.includedQuantity();
        int quantity = billableQuantity(item.itemType(), contractedQuantity, included);
        BigDecimal percent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal gross = grossOf(price.unitAmount(), quantity);
        BigDecimal discount = Money.percentOf(gross, percent);
        BigDecimal taxableBase = gross.subtract(discount);
        BigDecimal tax = price.taxTreatment() == TaxTreatment.TAXED
                ? Money.percentOf(taxableBase, price.taxRate())
                : Money.zero();
        BigDecimal total = taxableBase.add(tax);
        return new QuoteLine(null, lineNumber, item.id(), item.code(), item.name(), item.itemType(),
                contractedQuantity, included, quantity, price.unitAmount(), percent, discount,
                price.taxRate(), price.taxTreatment(), tax, total, createdDate, true);
    }

    /**
     * Regla R15: lo que ya viene incluido se resta ANTES de fijar la cantidad, con
     * suelo en cero.
     *
     * <p>
     * El caso exacto: Ana trabaja sola, responde "1 persona", el nucleo ya incluye
     * dos usuarios, y la linea de usuarios extra tiene que dar CERO -no una-. Sin
     * esta resta se le cobra al cliente una unidad que venia incluida, y eso no lo
     * detecta ninguna constraint: la cantidad es valida, el precio es valido, y la
     * factura sale mas alta de lo que debia.
     *
     * <p>
     * <b>Vive en el dominio y no en el servicio</b> a proposito. La resta la tiene
     * que hacer quien resuelve el precio -{@code included_quantity} esta en
     * {@code catalog_prices}, o sea en la tarifa- y el configurador no lee ni un
     * campo del articulo; dejarla en un servicio la volveria a poner al alcance de
     * quien la olvide. Aqui la comprueba ademas {@link #validateQuantities} en cada
     * construccion, lectura incluida.
     *
     * <p>
     * Solo se resta en las lineas de CAPACITY: un modulo no tiene unidades
     * incluidas que restar, y restarselas lo borraria de la oferta.
     *
     * @return unidades a cobrar; cero si lo contratado no supera lo incluido, nunca
     *         un negativo
     */
    public static int billableQuantity(QuoteItemType itemType, int contractedQuantity,
            int includedQuantity) {
        if (itemType != QuoteItemType.CAPACITY) {
            return contractedQuantity;
        }
        return Math.max(contractedQuantity - includedQuantity, 0);
    }

    /** Importe bruto del renglon: precio unitario congelado por la cantidad. */
    public BigDecimal grossAmount() {
        return grossOf(unitAmount, quantity);
    }

    private static BigDecimal grossOf(BigDecimal unitAmount, int quantity) {
        return Money.multiply(unitAmount, BigDecimal.valueOf(quantity));
    }

    private static void validateIdentity(int lineNumber, Long catalogItemId, String itemCode,
            String itemName, QuoteItemType itemType, int quantity) {
        if (lineNumber <= 0)
            throw new IllegalArgumentException("lineNumber must be positive");
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (itemCode == null || itemCode.isBlank())
            throw new IllegalArgumentException("itemCode is required");
        if (itemCode.length() > MAX_CODE)
            throw new IllegalArgumentException("itemCode must be 50 chars or less");
        if (itemName == null || itemName.isBlank())
            throw new IllegalArgumentException("itemName is required");
        if (itemName.length() > MAX_NAME)
            throw new IllegalArgumentException("itemName must be 120 chars or less");
        if (itemType == null)
            throw new IllegalArgumentException("itemType is required");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be positive");
    }

    /**
     * Las tres cifras tienen que contar la misma historia: lo contratado, lo que
     * venia incluido y lo que se cobra. Se comprueba tambien AL LEER, asi que una
     * fila editada por SQL para cobrar de mas se delata sola en vez de esperar a
     * que el cliente reclame.
     */
    private static void validateQuantities(QuoteItemType itemType, int contractedQuantity,
            int includedQuantity, int quantity) {
        if (contractedQuantity <= 0)
            throw new IllegalArgumentException("contractedQuantity must be positive");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
        int expected = billableQuantity(itemType, contractedQuantity, includedQuantity);
        if (quantity != expected)
            throw new QuoteLineArithmeticException("quantity", BigDecimal.valueOf(quantity),
                    BigDecimal.valueOf(expected));
    }

    private static void validateAmounts(BigDecimal unitAmount, BigDecimal discountPercent,
            BigDecimal discountAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
            BigDecimal taxAmount, BigDecimal lineTotal) {
        requireNotNegative(unitAmount, "unitAmount");
        requireNotNegative(discountAmount, "discountAmount");
        requireNotNegative(taxAmount, "taxAmount");
        requireNotNegative(lineTotal, "lineTotal");
        requirePercentage(discountPercent, "discountPercent");
        requirePercentage(taxRate, "taxRate");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        // Misma coherencia que chk_catalog_prices_tax_coherence, arrastrada a la
        // copia congelada: un EXCLUDED con tarifa 19 produce un IVA sobre una base
        // que no debia llevarlo, y eso solo aparece en la declaracion bimestral.
        if (taxTreatment == TaxTreatment.TAXED && taxRate.signum() <= 0)
            throw new IllegalArgumentException("TAXED line requires a positive taxRate");
        if (taxTreatment != TaxTreatment.TAXED && taxRate.signum() != 0)
            throw new IllegalArgumentException(
                    "non TAXED line requires taxRate = 0, got " + taxRate);
    }

    private static void requireNotNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0)
            throw new IllegalArgumentException(name + " must be zero or positive");
    }

    private static void requirePercentage(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0)
            throw new IllegalArgumentException(name + " must be between 0 and 100");
    }

    /**
     * Vuelve a hacer la cuenta y compara. Se usa {@code compareTo} y no
     * {@code equals} a proposito: {@code BigDecimal.equals} distingue 0.00 de 0.0,
     * y lo que aqui importa es el valor, no la escala con la que volvio del driver.
     */
    private void verifyArithmetic() {
        BigDecimal gross = grossAmount();
        if (discountAmount.compareTo(gross) > 0)
            throw new QuoteLineArithmeticException("discountAmount", discountAmount, gross);
        if (discountPercent.signum() > 0) {
            BigDecimal expected = Money.percentOf(gross, discountPercent);
            if (discountAmount.compareTo(expected) != 0)
                throw new QuoteLineArithmeticException("discountAmount", discountAmount, expected);
        }
        BigDecimal taxableBase = gross.subtract(discountAmount);
        BigDecimal expectedTax = taxTreatment == TaxTreatment.TAXED
                ? Money.percentOf(taxableBase, taxRate)
                : Money.zero();
        if (taxAmount.compareTo(expectedTax) != 0)
            throw new QuoteLineArithmeticException("taxAmount", taxAmount, expectedTax);
        BigDecimal expectedTotal = taxableBase.add(taxAmount);
        if (lineTotal.compareTo(expectedTotal) != 0)
            throw new QuoteLineArithmeticException("lineTotal", lineTotal, expectedTotal);
    }

    public Long getId() {
        return id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public QuoteItemType getItemType() {
        return itemType;
    }

    /** Unidades que el cliente pidio, antes de restar lo incluido. */
    public int getContractedQuantity() {
        return contractedQuantity;
    }

    /**
     * Unidades que la tarifa traia incluidas, COPIADAS al congelar la linea igual
     * que el precio o el nombre. No se releen de {@code catalog_prices} al pintar
     * la cotizacion: editar un tramo de la tarifa no puede cambiar retroactivamente
     * cuantas unidades le sobraban a quien firmo hace un ano.
     */
    public int getIncludedQuantity() {
        return includedQuantity;
    }

    /**
     * Unidades que se COBRAN: lo contratado menos lo incluido, con suelo en cero.
     */
    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
