package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Una linea del contrato, con sus fechas.
 *
 * <p>
 * <strong>{@code current_item_marker} no se mapea</strong>, por lo mismo que
 * {@code active_marker}: es una columna generada {@code STORED} que la base
 * calcula sola y que solo existe para sostener
 * {@code uq_subscription_items_current}.
 *
 * <p>
 * {@code created_amendment_id} y {@code ended_amendment_id} son columnas
 * {@code BIGINT} planas y no asociaciones. Sus FK en la base son
 * <strong>compuestas</strong> —{@code (company_id, amendment_id)}— y una FK
 * compuesta que comparte columna con otra asociacion no se mapea limpio en JPA;
 * lo que si hace falta de ellas es el id, y eso es lo que hay.
 */
@Entity
@Table(name = "subscription_items")
@SQLDelete(sql = "UPDATE subscription_items SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class SubscriptionItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 120)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private SubscriptionItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_unit", length = 30)
    private CapacityUnit capacityUnit;

    /** Congelada al firmar. Nunca se recalcula desde la tarifa. */
    @Column(name = "included_quantity", nullable = false)
    private int includedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** El precio congelado del cliente. Cambiar de precio es cerrar y abrir. */
    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Vacio = vigente ahora. Dar de baja es escribir aqui. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 25)
    private ItemOrigin origin;

    @Column(name = "created_amendment_id")
    private Long createdAmendmentId;

    @Column(name = "ended_amendment_id")
    private Long endedAmendmentId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected SubscriptionItemJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public SubscriptionJpaEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionJpaEntity subscription) {
        this.subscription = subscription;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
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

    public SubscriptionItemType getItemType() {
        return itemType;
    }

    public void setItemType(SubscriptionItemType itemType) {
        this.itemType = itemType;
    }

    public CapacityUnit getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(CapacityUnit capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    public int getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(int includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public void setTaxTreatment(TaxTreatment taxTreatment) {
        this.taxTreatment = taxTreatment;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public ItemOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(ItemOrigin origin) {
        this.origin = origin;
    }

    public Long getCreatedAmendmentId() {
        return createdAmendmentId;
    }

    public void setCreatedAmendmentId(Long createdAmendmentId) {
        this.createdAmendmentId = createdAmendmentId;
    }

    public Long getEndedAmendmentId() {
        return endedAmendmentId;
    }

    public void setEndedAmendmentId(Long endedAmendmentId) {
        this.endedAmendmentId = endedAmendmentId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
