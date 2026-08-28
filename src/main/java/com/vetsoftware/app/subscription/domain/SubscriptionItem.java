package com.vetsoftware.app.subscription.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que un cliente tiene contratado, <strong>con fechas</strong>. La tabla mas
 * importante del modelo, y la que mas facil se implementa mal.
 *
 * <p>
 * Tres reglas que esta clase impone por construccion, y que son el motivo de
 * que casi todos sus campos sean {@code final} y no haya ni un mutador:
 *
 * <ol>
 * <li><strong>Dar de baja no borra.</strong> Quitar un modulo es
 * {@link #endOn(LocalDate, Long)} —escribir {@code effective_to}—, jamas
 * eliminar la fila ni desactivarla. La informacion de que ese cliente tuvo ese
 * modulo entre marzo y septiembre es suya y no se destruye.
 * <li><strong>El precio no se edita jamas.</strong> {@code unitAmount} es
 * {@code final}. Cambiar de precio es cerrar esta linea y abrir otra
 * ({@link #withQuantity}), de forma que el periodo ya devengado siga
 * facturandose al precio que estaba firmado entonces.
 * <li><strong>{@code includedQuantity} va congelada al firmar.</strong> Es la
 * causa numero uno de sobrefacturacion en modelos de suscripcion: si lo
 * incluido se leyera de la tarifa vigente, editar un tramo cambiaria
 * retroactivamente cuantos usuarios le sobran a quien firmo hace un ano, y la
 * factura del mes siguiente le cobraria unidades que su contrato le regalaba.
 * Por eso se copia a la fila y no se vuelve a mirar la tarifa.
 * </ol>
 *
 * <p>
 * La vigencia no vive aqui: vive en {@link EffectivePeriod}, que es el unico
 * sitio del slice donde se escribe que significa «vigente».
 */
public class SubscriptionItem {

    private static final int MAX_CODE_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_AMOUNT_SCALE = 2;
    /**
     * El ancho de {@code limit_dimensions.code}, que es lo que la columna guarda.
     */
    private static final int MAX_CAPACITY_UNIT_LENGTH = 50;

    private final Long id;
    private final Long companyId;
    private final Long subscriptionId;
    private final Long catalogItemId;
    private final String itemCode;
    private final String itemName;
    private final SubscriptionItemType itemType;
    /**
     * El codigo del eje ({@code limit_dimensions.code}) congelado al firmar, no un
     * enumerado: desde el 333 la columna va atada por clave foranea al catalogo de
     * ejes y admite los ocho, no cuatro (#655).
     */
    private final String capacityUnit;
    /**
     * D-66: el tramo que esta linea cubre. Los tramos son ACUMULATIVOS, asi que una
     * cantidad escalonada se firma como varias lineas del mismo articulo a precios
     * distintos, y {@code uq_subscription_items_current} ya las admite por tramo.
     * {@code tierMax} vacio es "de ahi en adelante".
     *
     * <p>
     * Las dos columnas existian desde el 244 y <b>nadie las escribia</b>: eran
     * columna muerta, porque el unico camino de precio devolvia un tramo solo. Se
     * escriben desde aqui.
     */
    private final int tierMin;
    private final Integer tierMax;
    private final int includedQuantity;
    private final TaxTreatment taxTreatment;
    private final int quantity;
    private final BigDecimal unitAmount;
    /**
     * D-86: el descuento negociado, congelado como todo lo demas. La MARCA es lo
     * que la auditoria echaba en falta: sin ella el impuesto se liquida sobre el
     * precio rebajado, y la norma solo excluye de la base del IVA los descuentos
     * "no sujetos a ninguna condicion".
     */
    private final BigDecimal discountPercent;
    private final BigDecimal discountAmount;
    private final boolean discountIsConditional;
    private final BigDecimal taxRate;
    private EffectivePeriod period;
    private final ItemOrigin origin;
    private final Long createdAmendmentId;
    private Long endedAmendmentId;
    private final LocalDateTime createdDate;
    private final Long version;
    private final boolean enabled;

    public SubscriptionItem(Long id, Long companyId, Long subscriptionId, Long catalogItemId,
            String itemCode, String itemName, SubscriptionItemType itemType, String capacityUnit,
            int tierMin, Integer tierMax, int includedQuantity, TaxTreatment taxTreatment,
            int quantity, BigDecimal unitAmount, BigDecimal discountPercent,
            BigDecimal discountAmount, boolean discountIsConditional, BigDecimal taxRate,
            EffectivePeriod period, ItemOrigin origin, Long createdAmendmentId,
            Long endedAmendmentId, LocalDateTime createdDate, Long version, boolean enabled) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (itemCode == null || itemCode.isBlank())
            throw new IllegalArgumentException("itemCode is required");
        if (itemCode.length() > MAX_CODE_LENGTH)
            throw new IllegalArgumentException(
                    "itemCode must be " + MAX_CODE_LENGTH + " chars or less");
        if (itemName == null || itemName.isBlank())
            throw new IllegalArgumentException("itemName is required");
        if (itemName.length() > MAX_NAME_LENGTH)
            throw new IllegalArgumentException(
                    "itemName must be " + MAX_NAME_LENGTH + " chars or less");
        if (itemType == null)
            throw new IllegalArgumentException("itemType is required");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        // chk_subscription_items_capacity_unit, en los dos sentidos: una capacidad sin
        // unidad no se puede contar, y una unidad colgada de un modulo no significa
        // nada. Es la MITAD ESTRUCTURAL de la restriccion, la unica que sobrevivio al
        // 333: la otra mitad -que el valor sea un eje real- ya no es una lista de
        // cuatro literales sino una clave foranea contra limit_dimensions(code), y
        // comprobarla es una consulta que vive en el caso de uso, no aqui.
        if (itemType == SubscriptionItemType.CAPACITY
                && (capacityUnit == null || capacityUnit.isBlank()))
            throw new IllegalArgumentException("capacityUnit is required for a CAPACITY item");
        if (itemType != SubscriptionItemType.CAPACITY && capacityUnit != null)
            throw new IllegalArgumentException("capacityUnit is only valid for a CAPACITY item");
        if (capacityUnit != null && capacityUnit.length() > MAX_CAPACITY_UNIT_LENGTH)
            throw new IllegalArgumentException(
                    "capacityUnit must be " + MAX_CAPACITY_UNIT_LENGTH + " chars or less");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity must not be negative");
        // chk_subscription_items_tier, arrastrado al dominio: un tramo que arranca en
        // cero o que termina antes de empezar no es un tramo.
        if (tierMin < 1)
            throw new IllegalArgumentException("tierMin must be 1 or greater");
        if (tierMax != null && tierMax < tierMin)
            throw new IllegalArgumentException("tierMax must not be lower than tierMin");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be greater than zero");
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount must not be negative");
        if (unitAmount.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException("unitAmount must have at most two decimals");
        // chk_subscription_items_discount. El importe y el porcentaje se guardan los
        // dos: saber que se negocio y cuanto costo es lo que permite reconstruir la
        // base imponible de una linea firmada hace un ano sin volver a la tarifa.
        if (discountPercent == null || discountPercent.signum() < 0
                || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("discountPercent must be between 0 and 100");
        if (discountAmount == null || discountAmount.signum() < 0)
            throw new IllegalArgumentException("discountAmount must not be negative");
        if (discountAmount.compareTo(Money.multiply(BigDecimal.valueOf(quantity), unitAmount)) > 0)
            throw new IllegalArgumentException("discountAmount cannot exceed the line gross");
        if (taxRate == null || taxRate.signum() < 0
                || taxRate.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
        if (period == null)
            throw new IllegalArgumentException("effective period is required");
        if (origin == null)
            throw new IllegalArgumentException("origin is required");
        // chk_subscription_items_ended: o esta cerrada de verdad, o no tiene documento
        // de cierre. Una linea «cerrada por el otrosi 12» sin fecha de fin es un
        // contrato que se contradice a si mismo.
        if (endedAmendmentId != null && period.isOpen())
            throw new IllegalArgumentException(
                    "endedAmendmentId requires an effectiveTo on the line");
        this.id = id;
        this.companyId = companyId;
        this.subscriptionId = subscriptionId;
        this.catalogItemId = catalogItemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.capacityUnit = capacityUnit;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.includedQuantity = includedQuantity;
        this.taxTreatment = taxTreatment;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.discountPercent = Money.scaled(discountPercent);
        this.discountAmount = Money.scaled(discountAmount);
        this.discountIsConditional = discountIsConditional;
        this.taxRate = taxRate;
        this.period = period;
        this.origin = origin;
        this.createdAmendmentId = createdAmendmentId;
        this.endedAmendmentId = endedAmendmentId;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    /**
     * La linea de <b>tramo unico y abierto</b> {@code [1, infinito)} y sin
     * descuento, que es lo que describe cualquier articulo sin escalones y todo lo
     * firmado hasta hoy.
     *
     * <p>
     * No es un atajo: el tramo y el descuento son datos de la linea, y esta
     * sobrecarga solo nombra el caso en el que valen lo que valen por defecto. Los
     * caminos que resuelven precio usan la completa, porque son los unicos que
     * pueden saber que tramo aplica.
     */
    public SubscriptionItem(Long id, Long companyId, Long subscriptionId, Long catalogItemId,
            String itemCode, String itemName, SubscriptionItemType itemType, String capacityUnit,
            int includedQuantity, TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount,
            BigDecimal taxRate, EffectivePeriod period, ItemOrigin origin, Long createdAmendmentId,
            Long endedAmendmentId, LocalDateTime createdDate, Long version, boolean enabled) {
        this(id, companyId, subscriptionId, catalogItemId, itemCode, itemName, itemType,
                capacityUnit, 1, null, includedQuantity, taxTreatment, quantity, unitAmount,
                BigDecimal.ZERO, BigDecimal.ZERO, false, taxRate, period, origin,
                createdAmendmentId, endedAmendmentId, createdDate, version, enabled);
    }

    /** Abre una linea de tramo unico y abierto, sin descuento. */
    public static SubscriptionItem open(Long companyId, Long subscriptionId, Long catalogItemId,
            String itemCode, String itemName, SubscriptionItemType itemType, String capacityUnit,
            int includedQuantity, TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount,
            BigDecimal taxRate, EffectivePeriod period, ItemOrigin origin,
            Long createdAmendmentId) {
        return open(companyId, subscriptionId, catalogItemId, itemCode, itemName, itemType,
                capacityUnit, 1, null, includedQuantity, taxTreatment, quantity, unitAmount,
                BigDecimal.ZERO, BigDecimal.ZERO, false, taxRate, period, origin,
                createdAmendmentId);
    }

    /**
     * Abre una linea nueva congelando en ella el codigo, el nombre, el tipo, el
     * tratamiento fiscal, el precio unitario, la tarifa de IVA y —sobre todo— lo
     * incluido. A partir de aqui la tarifa puede cambiar cuanto quiera: este
     * cliente ya no la mira.
     */
    public static SubscriptionItem open(Long companyId, Long subscriptionId, Long catalogItemId,
            String itemCode, String itemName, SubscriptionItemType itemType, String capacityUnit,
            int tierMin, Integer tierMax, int includedQuantity, TaxTreatment taxTreatment,
            int quantity, BigDecimal unitAmount, BigDecimal discountPercent,
            BigDecimal discountAmount, boolean discountIsConditional, BigDecimal taxRate,
            EffectivePeriod period, ItemOrigin origin, Long createdAmendmentId) {
        return new SubscriptionItem(null, companyId, subscriptionId, catalogItemId, itemCode,
                itemName, itemType, capacityUnit, tierMin, tierMax, includedQuantity, taxTreatment,
                quantity, unitAmount, discountPercent, discountAmount, discountIsConditional,
                taxRate, period, origin, createdAmendmentId, null, null, null, true);
    }

    /**
     * Da de baja la linea: escribe {@code effective_to} y el otrosi que la cerro.
     * <strong>Ni borra la fila ni toca {@code enabled}.</strong> R12: dar de baja
     * un modulo jamas destruye informacion del cliente; lo que baja es el nivel de
     * acceso, y eso lo decide el recalculo de permisos, no esta clase.
     */
    public void endOn(LocalDate effectiveTo, Long amendmentId) {
        if (effectiveTo == null)
            throw new IllegalArgumentException("effectiveTo is required");
        if (!period.isOpen())
            throw new SubscriptionItemAlreadyEndedException(id);
        this.period = period.endingOn(effectiveTo);
        this.endedAmendmentId = amendmentId;
    }

    /**
     * La linea sucesora con otra cantidad. <strong>Cambiar de cantidad es cerrar y
     * abrir</strong>, nunca editar: la nueva arrastra intactos el precio unitario y
     * lo incluido de la original, porque lo que se renegocio fue cuantas unidades,
     * no a que precio.
     *
     * <p>
     * Devuelve la sucesora sin cerrar esta: cerrarla es responsabilidad del caso de
     * uso, que ademas tiene que emitir el otrosi y comprobar el solape.
     */
    public SubscriptionItem withQuantity(int newQuantity, LocalDate from, Long amendmentId) {
        // El descuento en pesos se reescala con la cantidad nueva: es un porcentaje
        // negociado y no un importe fijo, y arrastrarlo tal cual dejaria un descuento
        // mayor que el bruto -que el constructor rechaza- en cuanto la cantidad baje.
        BigDecimal newGross = Money.multiply(BigDecimal.valueOf(newQuantity), unitAmount);
        return open(companyId, subscriptionId, catalogItemId, itemCode, itemName, itemType,
                capacityUnit, tierMin, tierMax, includedQuantity, taxTreatment, newQuantity,
                unitAmount, discountPercent, Money.percentOf(newGross, discountPercent),
                discountIsConditional, taxRate, EffectivePeriod.openFrom(from),
                ItemOrigin.QUANTITY_CHANGE, amendmentId);
    }

    /**
     * ¿Estaba vigente ese dia? Delega en {@link EffectivePeriod#isCurrentOn}, que
     * es donde vive la definicion. {@code enabled} cuenta porque
     * {@code @SQLRestriction} tambien lo cuenta.
     */
    public boolean isCurrentOn(LocalDate day) {
        return enabled && period.isCurrentOn(day);
    }

    /** ¿Se pisa con ese tramo? Delega en {@link EffectivePeriod#overlaps}. */
    public boolean overlaps(EffectivePeriod other) {
        return enabled && period.overlaps(other);
    }

    /**
     * Las unidades que de verdad se cobran: lo contratado menos lo que el contrato
     * ya incluia, y nunca menos que cero. Usa la copia congelada, no la tarifa
     * vigente — que es justamente lo que evita cobrarle a alguien una unidad que le
     * venia incluida cuando firmo.
     */
    public int billableQuantity() {
        return Math.max(quantity - includedQuantity, 0);
    }

    /**
     * Lo que esta linea aporta a la cuota recurrente de un ciclo:
     * {@code billableQuantity x unit_amount}, <strong>sin impuestos</strong> —igual
     * que {@code subscription_charges.subtotal_amount}, porque el impuesto lo
     * desglosa el documento y no el cargo—.
     *
     * <p>
     * Es el insumo del prorrateo, y usa el precio y lo incluido <em>congelados en
     * la fila</em>, no la tarifa vigente: prorratear contra la tarifa de hoy
     * cobraria unidades que el contrato firmado regalaba.
     */
    public BigDecimal recurringSubtotal() {
        return recurringSubtotalOf(quantity, includedQuantity, unitAmount).subtract(discountAmount);
    }

    /**
     * D-86 / R-TAX-04. La base sobre la que se liquida el impuesto de esta linea:
     * el bruto menos el descuento, <b>salvo cuando el descuento esta
     * CONDICIONADO</b>, y entonces el bruto entero.
     *
     * <p>
     * Un veinte por ciento a cambio de quedarse doce meses esta condicionado por
     * definicion, y la norma solo excluye de la base del IVA los descuentos "no
     * sujetos a ninguna condicion". Sobre 179.000 son 34.010 de impuesto y no
     * 27.208: 816.240 en un semestre con veinte clientes rebajados.
     */
    public BigDecimal taxableBase() {
        BigDecimal gross = recurringSubtotalOf(quantity, includedQuantity, unitAmount);
        return discountIsConditional ? gross : gross.subtract(discountAmount);
    }

    /**
     * La misma cuenta para una linea que <strong>todavia no existe</strong>: el
     * alta, cuya fila no se puede construir hasta tener el id del otrosi, y la
     * sucesora de un cambio de cantidad. Sin esta sobrecarga el caso de uso
     * duplicaria la formula, que es como empiezan las dos aritmeticas que no
     * cuadran.
     */
    public static BigDecimal recurringSubtotalOf(int quantity, int includedQuantity,
            BigDecimal unitAmount) {
        if (unitAmount == null)
            return Money.zero();
        return Money.multiply(BigDecimal.valueOf(Math.max(quantity - includedQuantity, 0)),
                unitAmount);
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
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

    public SubscriptionItemType getItemType() {
        return itemType;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    /** Primera unidad facturable que cubre esta linea. Arranca en 1. */
    public int getTierMin() {
        return tierMin;
    }

    /** Ultima unidad cubierta, o vacio para "de ahi en adelante". */
    public Integer getTierMax() {
        return tierMax;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    /**
     * D-86: si el descuento esta sujeto a condicion, el impuesto se liquida sobre
     * el precio de lista. La marca nace en el renglon de la cotizacion y llega
     * hasta aqui congelada: ya no muere ahi.
     */
    public boolean isDiscountConditional() {
        return discountIsConditional;
    }

    public int getIncludedQuantity() {
        return includedQuantity;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public EffectivePeriod getPeriod() {
        return period;
    }

    public ItemOrigin getOrigin() {
        return origin;
    }

    public Long getCreatedAmendmentId() {
        return createdAmendmentId;
    }

    public Long getEndedAmendmentId() {
        return endedAmendmentId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
