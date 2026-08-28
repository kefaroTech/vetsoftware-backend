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
    private final int tierMin;
    private final Integer tierMax;
    private final int contractedQuantity;
    private final int includedQuantity;
    private final int quantity;
    private final BigDecimal unitAmount;
    private final BigDecimal discountPercent;
    private final BigDecimal discountAmount;
    private final boolean discountIsConditional;
    private final BigDecimal taxRate;
    private final TaxTreatment taxTreatment;
    private final BigDecimal taxAmount;
    private final BigDecimal lineTotal;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public QuoteLine(Long id, int lineNumber, Long catalogItemId, String itemCode, String itemName,
            QuoteItemType itemType, int tierMin, Integer tierMax, int contractedQuantity,
            int includedQuantity, int quantity, BigDecimal unitAmount, BigDecimal discountPercent,
            BigDecimal discountAmount, boolean discountIsConditional, BigDecimal taxRate,
            TaxTreatment taxTreatment, BigDecimal taxAmount, BigDecimal lineTotal,
            LocalDateTime createdDate, boolean enabled) {
        validateIdentity(lineNumber, catalogItemId, itemCode, itemName, itemType, quantity);
        validateTier(tierMin, tierMax);
        validateQuantities(itemType, tierMin, tierMax, contractedQuantity, includedQuantity,
                quantity);
        validateAmounts(unitAmount, discountPercent, discountAmount, taxRate, taxTreatment,
                taxAmount, lineTotal);
        this.id = id;
        this.lineNumber = lineNumber;
        this.catalogItemId = catalogItemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.contractedQuantity = contractedQuantity;
        this.includedQuantity = includedQuantity;
        this.quantity = quantity;
        this.unitAmount = Money.scaled(unitAmount);
        this.discountPercent = discountPercent.setScale(2, Money.ROUND);
        this.discountAmount = Money.scaled(discountAmount);
        this.discountIsConditional = discountIsConditional;
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
            int contractedQuantity, int includedQuantity, BigDecimal discountPercent,
            boolean discountIsConditional, LocalDateTime createdDate) {
        if (item == null)
            throw new IllegalArgumentException("catalog item is required");
        if (price == null)
            throw new IllegalArgumentException("catalog price is required");
        int quantity = tierQuantity(item.itemType(), price.tierMin(), price.tierMax(),
                contractedQuantity, includedQuantity);
        BigDecimal percent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal gross = grossOf(price.unitAmount(), quantity);
        BigDecimal discount = Money.percentOf(gross, percent);
        BigDecimal net = gross.subtract(discount);
        BigDecimal tax = price.taxTreatment() == TaxTreatment.TAXED
                ? Money.percentOf(taxableBaseOf(gross, net, discountIsConditional), price.taxRate())
                : Money.zero();
        return new QuoteLine(null, lineNumber, item.id(), item.code(), item.name(), item.itemType(),
                price.tierMin(), price.tierMax(), contractedQuantity, includedQuantity, quantity,
                price.unitAmount(), percent, discount, discountIsConditional, price.taxRate(),
                price.taxTreatment(), tax, net.add(tax), createdDate, true);
    }

    /**
     * El renglon de <b>tramo unico y abierto</b> {@code [1, infinito)} y sin
     * descuento condicionado, que es lo que describe cualquier articulo del
     * catalogo sin escalones y todo descuento que hoy se vende.
     *
     * <p>
     * No es un atajo para saltarse nada: el tramo y la marca son datos de la linea,
     * y esta sobrecarga solo nombra el caso en el que valen lo que valen por
     * defecto. Los caminos que resuelven precio -{@code CreateQuoteService}- usan
     * la completa, porque son los unicos que pueden saber que tramo aplica.
     */
    public QuoteLine(Long id, int lineNumber, Long catalogItemId, String itemCode, String itemName,
            QuoteItemType itemType, int contractedQuantity, int includedQuantity, int quantity,
            BigDecimal unitAmount, BigDecimal discountPercent, BigDecimal discountAmount,
            BigDecimal taxRate, TaxTreatment taxTreatment, BigDecimal taxAmount,
            BigDecimal lineTotal, LocalDateTime createdDate, boolean enabled) {
        this(id, lineNumber, catalogItemId, itemCode, itemName, itemType, 1, null,
                contractedQuantity, includedQuantity, quantity, unitAmount, discountPercent,
                discountAmount, false, taxRate, taxTreatment, taxAmount, lineTotal, createdDate,
                enabled);
    }

    /**
     * D-86 / R-TAX-04. <b>Un descuento CONDICIONADO no reduce la base del IVA.</b>
     *
     * <p>
     * La norma es literal: no forman parte de la base los descuentos "siempre y
     * cuando no esten sujetos a ninguna condicion". Un veinte por ciento a cambio
     * de quedarse doce meses esta condicionado por definicion, asi que el impuesto
     * se liquida sobre el precio de lista y no sobre el rebajado. Sobre 179.000 con
     * un 20 % de permanencia son 34.010 de IVA y no 27.208: <b>6.802 por cliente y
     * mes, 816.240 en un semestre con veinte clientes rebajados</b>, que es lo que
     * este metodo existe para no dejar de liquidar.
     *
     * <p>
     * Lo que el cliente PAGA sigue siendo el neto mas el impuesto; lo que cambia es
     * sobre que se calcula el impuesto. Por eso el total de la linea se arma con
     * {@code net + tax} y no con {@code base + tax}: con descuento incondicionado
     * -que hoy es todo el catalogo- las dos formulas dan el mismo numero.
     */
    private static BigDecimal taxableBaseOf(BigDecimal gross, BigDecimal net,
            boolean discountIsConditional) {
        return discountIsConditional ? gross : net;
    }

    /**
     * Congela un renglon de <b>tramo unico</b> tomando lo incluido del propio
     * precio y sin descuento condicionado. Es el caso del catalogo sin escalones.
     */
    public static QuoteLine freeze(int lineNumber, CatalogItemRef item, CatalogPriceRef price,
            int contractedQuantity, BigDecimal discountPercent, LocalDateTime createdDate) {
        if (price == null)
            throw new IllegalArgumentException("catalog price is required");
        return freeze(lineNumber, item, price, contractedQuantity, price.includedQuantity(),
                discountPercent, false, createdDate);
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

    /**
     * D-66 / R-PRICE-04: las unidades de ESTE TRAMO. Es {@link #billableQuantity}
     * recortado a la ventana {@code [tierMin, tierMax]}, y es lo que convierte
     * trece unidades facturables en ocho renglon uno y cinco renglon dos en vez de
     * trece al precio del tramo alto.
     *
     * <p>
     * <b>Se comprueba tambien AL LEER</b>, igual que el resto de la aritmetica de
     * la linea: una fila editada por SQL para cobrar el tramo entero al precio
     * barato se delata sola en vez de esperar a la reclamacion.
     *
     * <p>
     * Con el tramo unico y abierto -{@code [1, infinito)}, que es como esta todo el
     * catalogo sin escalones- devuelve exactamente lo facturable, asi que la regla
     * vieja sigue siendo cierta como caso particular de esta.
     */
    public static int tierQuantity(QuoteItemType itemType, int tierMin, Integer tierMax,
            int contractedQuantity, int includedQuantity) {
        int billable = billableQuantity(itemType, contractedQuantity, includedQuantity);
        if (billable < tierMin)
            return 0;
        int upper = tierMax == null ? billable : Math.min(billable, tierMax);
        return upper - tierMin + 1;
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
    private static void validateQuantities(QuoteItemType itemType, int tierMin, Integer tierMax,
            int contractedQuantity, int includedQuantity, int quantity) {
        if (contractedQuantity <= 0)
            throw new IllegalArgumentException("contractedQuantity must be positive");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
        int expected = tierQuantity(itemType, tierMin, tierMax, contractedQuantity,
                includedQuantity);
        if (quantity != expected)
            throw new QuoteLineArithmeticException("quantity", BigDecimal.valueOf(quantity),
                    BigDecimal.valueOf(expected));
    }

    private static void validateTier(int tierMin, Integer tierMax) {
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be 1 or greater");
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException("tierMax must not be lower than tierMin");
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
        BigDecimal net = gross.subtract(discountAmount);
        BigDecimal expectedTax = taxTreatment == TaxTreatment.TAXED
                ? Money.percentOf(taxableBaseOf(gross, net, discountIsConditional), taxRate)
                : Money.zero();
        if (taxAmount.compareTo(expectedTax) != 0)
            throw new QuoteLineArithmeticException("taxAmount", taxAmount, expectedTax);
        BigDecimal expectedTotal = net.add(taxAmount);
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

    /** Primera unidad facturable que cubre este renglon. Arranca en 1. */
    public int getTierMin() {
        return tierMin;
    }

    /** Ultima unidad cubierta, o vacio para "de ahi en adelante". */
    public Integer getTierMax() {
        return tierMax;
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

    /**
     * D-86: si el descuento esta sujeto a una condicion -permanencia, tipicamente-,
     * el impuesto se liquido sobre el precio de lista y no sobre el rebajado. Viaja
     * congelado a la linea del contrato: no muere aqui.
     */
    public boolean isDiscountConditional() {
        return discountIsConditional;
    }

    /**
     * La base sobre la que se liquido el impuesto de este renglon. Es el bruto
     * menos el descuento salvo cuando el descuento esta condicionado, y entonces es
     * el bruto entero.
     */
    public BigDecimal taxableBase() {
        BigDecimal gross = grossAmount();
        return taxableBaseOf(gross, gross.subtract(discountAmount), discountIsConditional);
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
