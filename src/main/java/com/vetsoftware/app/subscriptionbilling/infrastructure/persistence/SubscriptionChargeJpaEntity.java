package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo devengado.
 *
 * <p>
 * <b>Sin {@code @SQLDelete} y sin {@code @SQLRestriction}, porque la tabla no
 * lleva {@code enabled}.</b> Un cargo no se desactiva: se compensa con otro
 * negativo y los dos quedan. Con borrado lógico, la {@code @SQLRestriction}
 * escondería la mitad de la conciliación y el devengado del periodo dejaría de
 * cerrar sumando filas.
 *
 * <p>
 * <b>Sin {@code @Version}</b>, con la exención {@code E6_YA_PROTEGIDO}: el
 * importe nunca muta y el único {@code UPDATE} de la tabla es el sellado
 * {@code PENDING → INVOICED}, que corre dentro de la misma transacción que crea
 * el documento, ya versionado.
 *
 * <p>
 * <b>No hay columna de impuesto y es deliberado.</b> El cargo guarda su base
 * ({@code subtotal_amount}) y su tarifa; el importe del IVA se calcula una sola
 * vez sobre la base agregada y vive en
 * {@code subscription_billing_document_taxes}.
 *
 * <p>
 * <b>Por qué las referencias son ids pelados y no {@code @ManyToOne}.</b> El
 * aislamiento entre clínicas aquí no es una regla de código: es la clave
 * foránea, que <b>arrastra la empresa</b>
 * ({@code fk_subscription_charges_subscription} sobre
 * {@code (company_id, subscription_id)}, y lo mismo con la línea, el otrosí, el
 * documento y la autorreferencia de anulación). Mapear cualquiera de esas como
 * una asociación de una sola columna le pediría a Hibernate una FK simple y
 * <b>deshace la garantía</b> — que un pago de una clínica no pueda saldar la
 * factura de otra. Los datos del contrato se resuelven por
 * {@code SubscriptionQueryPort}, acotado por empresa; aquí solo viajan los ids.
 */
@Entity
@Table(name = "subscription_charges")
public class SubscriptionChargeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "subscription_item_id")
    private Long subscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 20)
    private ChargeType chargeType;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "service_period_start", nullable = false)
    private LocalDate servicePeriodStart;

    @Column(name = "service_period_end", nullable = false)
    private LocalDate servicePeriodEnd;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    /**
     * Con signo. Ver la convención de signos en {@code suscripciones-modelo.md} §3.
     */
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(name = "proration_days")
    private Integer prorationDays;

    @Column(name = "period_days")
    private Integer periodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "amendment_id")
    private Long amendmentId;

    @Column(name = "billing_document_id")
    private Long billingDocumentId;

    @Column(name = "voids_charge_id")
    private Long voidsChargeId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SubscriptionChargeJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getSubscriptionItemId() {
        return subscriptionItemId;
    }

    public void setSubscriptionItemId(Long subscriptionItemId) {
        this.subscriptionItemId = subscriptionItemId;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public void setChargeType(ChargeType chargeType) {
        this.chargeType = chargeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getServicePeriodStart() {
        return servicePeriodStart;
    }

    public void setServicePeriodStart(LocalDate servicePeriodStart) {
        this.servicePeriodStart = servicePeriodStart;
    }

    public LocalDate getServicePeriodEnd() {
        return servicePeriodEnd;
    }

    public void setServicePeriodEnd(LocalDate servicePeriodEnd) {
        this.servicePeriodEnd = servicePeriodEnd;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(BigDecimal unitAmount) {
        this.unitAmount = unitAmount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
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

    public Integer getProrationDays() {
        return prorationDays;
    }

    public void setProrationDays(Integer prorationDays) {
        this.prorationDays = prorationDays;
    }

    public Integer getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(Integer periodDays) {
        this.periodDays = periodDays;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    public Long getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(Long amendmentId) {
        this.amendmentId = amendmentId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public void setBillingDocumentId(Long billingDocumentId) {
        this.billingDocumentId = billingDocumentId;
    }

    public Long getVoidsChargeId() {
        return voidsChargeId;
    }

    public void setVoidsChargeId(Long voidsChargeId) {
        this.voidsChargeId = voidsChargeId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
