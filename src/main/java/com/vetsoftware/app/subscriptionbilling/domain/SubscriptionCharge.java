package com.vetsoftware.app.subscriptionbilling.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Lo devengado: <b>un servicio prestado en un periodo</b>, con o sin factura
 * todavía.
 *
 * <p>
 * <b>La regla que gobierna todo el slice y que este tipo hace estructural:
 * ninguna fila se edita ni se borra después de creada.</b> Todos los campos son
 * {@code final} salvo dos —{@link #status} y {@link #billingDocumentId}— que
 * son los únicos que la especificación permite mutar (R1). No hay setter de
 * importe, de cantidad, de tarifa ni de periodo, y no lo hay a propósito: si
 * existiera, se podría reescribir el pasado y ninguna auditoría valdría nada.
 * Anular es crear una fila que compensa, con {@link #voidingOf}, y <b>los dos
 * quedan</b>.
 *
 * <p>
 * <b>El cargo guarda su base, no su impuesto.</b> No existe aquí ningún
 * {@code taxAmount} y es deliberado: el importe del IVA se calcula una sola
 * vez, sobre la base agregada del documento, y vive en
 * {@code subscription_billing_document_taxes}. Guardarlo también aquí creaba
 * dos verdades que difieren en un peso —el descuadre que aparece en la
 * declaración bimestral—. Si alguien añade {@code taxAmount} a este tipo, ha
 * reabierto un bloqueante cerrado.
 *
 * <p>
 * <b>La mitad «cargo» de la convención de signos</b>
 * ({@code suscripciones-modelo.md} §3): {@link #subtotalAmount} va <b>con
 * signo</b>. Un cargo de anulación de −179.000 sumado al original de +179.000
 * da cero, y es la única forma de que el devengado de un periodo cierre sumando
 * filas. La otra mitad —el documento siempre positivo— vive en
 * {@link DocumentKind}.
 */
public final class SubscriptionCharge {

    private final Long id;
    private final Long companyId;
    private final Long subscriptionId;
    private final Long subscriptionItemId;
    private final ChargeType chargeType;
    private final String description;
    private final ServicePeriod servicePeriod;
    private final BigDecimal quantity;
    private final BigDecimal unitAmount;
    private final BigDecimal subtotalAmount;
    private final BigDecimal taxRate;
    private final TaxTreatment taxTreatment;
    private final ProrationBasis proration;
    private final Long amendmentId;
    private final Long voidsChargeId;
    private final LocalDateTime createdDate;

    private ChargeStatus status;
    private Long billingDocumentId;

    public SubscriptionCharge(Long id, Long companyId, Long subscriptionId, Long subscriptionItemId,
            ChargeType chargeType, String description, ServicePeriod servicePeriod,
            BigDecimal quantity, BigDecimal unitAmount, BigDecimal subtotalAmount,
            BigDecimal taxRate, TaxTreatment taxTreatment, ProrationBasis proration,
            ChargeStatus status, Long amendmentId, Long billingDocumentId, Long voidsChargeId,
            LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscriptionId is required");
        if (chargeType == null)
            throw new IllegalArgumentException("chargeType is required");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (servicePeriod == null)
            throw new IllegalArgumentException("servicePeriod is required");
        validarDescripcion(description);
        validarCantidades(quantity, unitAmount, subtotalAmount);
        taxTreatment.validarTarifa(taxRate);
        validarSigno(chargeType, subtotalAmount);
        validarAnulacion(chargeType, voidsChargeId);
        validarSellado(status, billingDocumentId);
        this.id = id;
        this.companyId = companyId;
        this.subscriptionId = subscriptionId;
        this.subscriptionItemId = subscriptionItemId;
        this.chargeType = chargeType;
        this.description = description;
        this.servicePeriod = servicePeriod;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.subtotalAmount = subtotalAmount;
        this.taxRate = taxRate;
        this.taxTreatment = taxTreatment;
        this.proration = proration;
        this.status = status;
        this.amendmentId = amendmentId;
        this.billingDocumentId = billingDocumentId;
        this.voidsChargeId = voidsChargeId;
        this.createdDate = createdDate;
    }

    /**
     * Devenga un cargo nuevo. Nace {@code PENDING} y sin documento: devengar y
     * facturar son dos cosas distintas y el servicio se devenga <b>aunque la
     * emisión falle</b>.
     *
     * <p>
     * El {@link Clock} entra por parámetro en vez de llamar a
     * {@code LocalDateTime.now()} aquí dentro: un cargo que cruza medianoche entre
     * dos líneas solo se reproduce en CI y de noche.
     */
    public static SubscriptionCharge create(Long companyId, Long subscriptionId,
            Long subscriptionItemId, ChargeType chargeType, String description,
            ServicePeriod servicePeriod, BigDecimal quantity, BigDecimal unitAmount,
            BigDecimal subtotalAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
            ProrationBasis proration, Long amendmentId, Clock clock) {
        return new SubscriptionCharge(null, companyId, subscriptionId, subscriptionItemId,
                chargeType, description, servicePeriod, Money.scaled(quantity),
                Money.scaled(unitAmount), Money.scaled(subtotalAmount), taxRate, taxTreatment,
                proration, ChargeStatus.PENDING, amendmentId, null, null, LocalDateTime.now(clock));
    }

    /**
     * El cargo de anulación: la fila negativa que compensa a {@code original} y lo
     * deja sumando cero. <b>Los dos quedan visibles</b>; el original no se toca
     * salvo su estado.
     *
     * <p>
     * <b>Lo que el esquema deja fuera, escrito porque es un límite real y no un
     * descuido de este método.</b> {@code chk_subscription_charges_voids} exige que
     * el cargo que apunta a otro sea de tipo {@code CREDIT}, y
     * {@code chk_subscription_charges_sign} exige que un {@code CREDIT} sea
     * negativo o cero. Las dos juntas significan que <b>solo se puede encadenar la
     * anulación de un cargo positivo</b>: compensar un {@code CREDIT} o un
     * {@code DISCOUNT} exigiría una fila positiva con {@code voidsChargeId}, y esa
     * combinación no cabe en la base. Se rechaza aquí, con el motivo delante, en
     * vez de dejar que salte la constraint convertida en un 500.
     */
    public static SubscriptionCharge voidingOf(SubscriptionCharge original, String description,
            Clock clock) {
        if (original == null)
            throw new IllegalArgumentException("original charge is required");
        if (original.id == null)
            throw new IllegalArgumentException("original charge must be persisted first");
        if (original.status == ChargeStatus.VOIDED)
            throw new IllegalArgumentException("charge " + original.id + " is already voided");
        if (original.subtotalAmount.signum() < 0)
            throw new IllegalArgumentException("charge " + original.id
                    + " is already negative: the schema only expresses the compensation of a"
                    + " positive charge (chk_subscription_charges_voids requires CREDIT and"
                    + " chk_subscription_charges_sign requires CREDIT to be non-positive)");
        SubscriptionCharge compensacion = new SubscriptionCharge(null, original.companyId,
                original.subscriptionId, original.subscriptionItemId, ChargeType.CREDIT,
                description, original.servicePeriod, original.quantity, original.unitAmount,
                original.subtotalAmount.negate(), original.taxRate, original.taxTreatment,
                original.proration, ChargeStatus.PENDING, original.amendmentId, null, original.id,
                LocalDateTime.now(clock));
        original.status = ChargeStatus.VOIDED;
        return compensacion;
    }

    /**
     * Sella el cargo dentro de un documento de cobro. Es una de las dos únicas
     * mutaciones permitidas, y va <b>en la misma transacción</b> que crea el
     * documento: un cargo {@code INVOICED} sin documento es un devengo que
     * desapareció del radar.
     */
    public void markInvoiced(Long billingDocumentId) {
        if (billingDocumentId == null)
            throw new IllegalArgumentException("billingDocumentId is required");
        if (status == ChargeStatus.INVOICED)
            throw new SubscriptionChargeAlreadyInvoicedException(id);
        if (status == ChargeStatus.VOIDED)
            throw new IllegalArgumentException("charge " + id + " is voided and cannot be billed");
        this.billingDocumentId = billingDocumentId;
        this.status = ChargeStatus.INVOICED;
    }

    /** {@code true} si el cargo todavía puede entrar en un documento de cobro. */
    public boolean esFacturable() {
        return status == ChargeStatus.PENDING;
    }

    /** El signo del subtotal: −1, 0 o 1. */
    public int signo() {
        return subtotalAmount.signum();
    }

    private static void validarDescripcion(String description) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");
        if (description.length() > 255)
            throw new IllegalArgumentException("description must be 255 chars or less");
    }

    private static void validarCantidades(BigDecimal quantity, BigDecimal unitAmount,
            BigDecimal subtotalAmount) {
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("quantity must be greater than zero");
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount cannot be negative");
        if (subtotalAmount == null)
            throw new IllegalArgumentException("subtotalAmount is required");
    }

    /**
     * Espejo de {@code chk_subscription_charges_sign}, que es la convención de
     * signos hecha esquema en el lado del cargo.
     *
     * <p>
     * {@code PRORATION} queda libre de signo a propósito: una ampliación a mitad de
     * ciclo cobra y una reducción acredita, y las dos son operaciones normales.
     */
    private static void validarSigno(ChargeType chargeType, BigDecimal subtotalAmount) {
        if (chargeType.exigeSubtotalNoNegativo() && subtotalAmount.signum() < 0)
            throw new IllegalArgumentException(
                    chargeType + " charges cannot have a negative subtotal");
        if (chargeType.exigeSubtotalNoPositivo() && subtotalAmount.signum() > 0)
            throw new IllegalArgumentException(
                    chargeType + " charges cannot have a positive subtotal");
    }

    /** Espejo de {@code chk_subscription_charges_voids}. */
    private static void validarAnulacion(ChargeType chargeType, Long voidsChargeId) {
        if (voidsChargeId != null && chargeType != ChargeType.CREDIT)
            throw new IllegalArgumentException(
                    "only a CREDIT charge can compensate another charge, but this one is "
                            + chargeType);
    }

    /** Espejo de {@code chk_subscription_charges_invoiced}. */
    private static void validarSellado(ChargeStatus status, Long billingDocumentId) {
        if (status == ChargeStatus.INVOICED && billingDocumentId == null)
            throw new IllegalArgumentException(
                    "an INVOICED charge must reference its billing document");
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

    public Long getSubscriptionItemId() {
        return subscriptionItemId;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public String getDescription() {
        return description;
    }

    public ServicePeriod getServicePeriod() {
        return servicePeriod;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public ProrationBasis getProration() {
        return proration;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public Long getAmendmentId() {
        return amendmentId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public Long getVoidsChargeId() {
        return voidsChargeId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
