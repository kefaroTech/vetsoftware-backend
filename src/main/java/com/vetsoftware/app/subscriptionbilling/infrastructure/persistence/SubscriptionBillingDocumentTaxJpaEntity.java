package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

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
import java.time.LocalDateTime;

/**
 * El desglose fiscal: una fila por {@code (tratamiento, tarifa)}.
 *
 * <p>
 * <b>Sin {@code @Version}</b> ({@code E1_APPEND_ONLY}): se escribe una vez al
 * cerrar el documento y el bloqueo vive en la cabecera, ya versionada. <b>Sin
 * {@code enabled}</b>: es la base declarable ante la DIAN, y ocultar una fila
 * cambiaría lo declarado sin dejar rastro. Por eso tampoco hay
 * {@code @SQLDelete} ni {@code @SQLRestriction}, y el repositorio no expone
 * ningún {@code delete}.
 *
 * <p>
 * {@code uq_sbdt_document_rate} es lo que hace cumplir el «una sola vez»: un
 * documento no puede tener dos bloques con el mismo tratamiento y la misma
 * tarifa, porque entonces la base declarada sería el doble.
 */
@Entity
@Table(name = "subscription_billing_document_taxes")
public class SubscriptionBillingDocumentTaxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "billing_document_id", nullable = false)
    private Long billingDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /**
     * La base sumada de las líneas con esa tarifa. <b>Esta suma es la que se
     * declara.</b>
     */
    @Column(name = "taxable_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableBase;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SubscriptionBillingDocumentTaxJpaEntity() {
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

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public void setBillingDocumentId(Long billingDocumentId) {
        this.billingDocumentId = billingDocumentId;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public void setTaxTreatment(TaxTreatment taxTreatment) {
        this.taxTreatment = taxTreatment;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxableBase() {
        return taxableBase;
    }

    public void setTaxableBase(BigDecimal taxableBase) {
        this.taxableBase = taxableBase;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
