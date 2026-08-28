package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
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

    /**
     * El <strong>codigo del eje</strong> ({@code limit_dimensions.code}) congelado
     * en la linea, no un enumerado. Desde el changeset 333 la columna lleva
     * {@code fk_subscription_items_capacity_unit} contra
     * {@code limit_dimensions(code)}, que es lo que sustituyo a la lista literal de
     * cuatro valores (#655).
     *
     * <p>
     * Es la columna que {@code ContractItemJpaRepository} ya cruzaba contra
     * {@code limit_dimensions.code} para calcular el techo: lo unico que cambia es
     * que ahora el esquema garantiza que el cruce encuentra fila. Antes se podia
     * firmar una unidad que el contador no supiera contar solo si alguien aflojaba
     * el {@code CHECK}; ahora no se puede firmar ninguna que no exista.
     */
    @Column(name = "capacity_unit", length = 50)
    private String capacityUnit;

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

    /**
     * Capas I y J. Diez columnas que nacen en el changeset 244 y que todavía no
     * llegan al agregado de dominio: el cableado -calcular la ventana, abrir la
     * fila sucesora, cobrar por tramos- es trabajo de esas capas. Se mapean aquí
     * con valor inicial porque son {@code NOT NULL} en la base y sin ellas
     * cualquier alta de línea fallaría en el motor.
     *
     * <p>
     * Los valores iniciales describen exactamente lo que el sistema hace HOY: toda
     * línea se cobra ({@code PAID}), ningún artículo se regala ({@code NEVER_FREE},
     * y por eso {@code maxTrialDays} en cero y {@code trialEndDate} vacío, que es
     * lo que la restricción exige), un solo tramo desde la unidad uno, importe
     * mensual, alta por plataforma y sin efecto de facturación. Son coherentes
     * entre sí frente a las seis restricciones nuevas de la tabla, no un relleno.
     *
     * <p>
     * <strong>{@code tierMin} nace en uno y nunca vacío</strong>, y no es estética:
     * la unicidad de línea viva pasa a mirar también el tramo, y una unicidad no
     * restringe filas con un vacío en cualquiera de sus columnas. Con el tramo
     * vacío, la regla dejaría de proteger justo el caso corriente -dos líneas vivas
     * del mismo módulo, facturado dos veces-.
     */
    @Column(name = "tier_min", nullable = false)
    private int tierMin = 1;

    /**
     * D-86 / R-QUOTE-05. El descuento negociado, CONGELADO, y sobre todo su marca
     * de condicionado. Hasta el 337 estas tres columnas no existian, asi que la
     * marca que nace en {@code quote_lines.discount_is_conditional} no tenia donde
     * llegar y moria en el renglon de la oferta: el impuesto acababa liquidandose
     * sobre el precio rebajado, y la norma solo excluye de la base del IVA los
     * descuentos "no sujetos a ninguna condicion".
     */
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_is_conditional", nullable = false)
    private boolean discountIsConditional = false;

    @Column(name = "tier_max")
    private Integer tierMax;

    @Column(name = "months_in_cycle", nullable = false)
    private int monthsInCycle = 1;

    @Column(name = "charge_mode", nullable = false, length = 20)
    private String chargeMode = "PAID";

    @Column(name = "trial_eligibility", nullable = false, length = 20)
    private String trialEligibility = "NEVER_FREE";

    @Column(name = "max_trial_days", nullable = false)
    private int maxTrialDays = 0;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "activation_path", nullable = false, length = 20)
    private String activationPath = "PLATFORM";

    @Column(name = "billing_effect", nullable = false, length = 20)
    private String billingEffect = "NONE";

    @Column(name = "succeeds_item_id")
    private Long succeedsItemId;

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

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
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

    public boolean isDiscountIsConditional() {
        return discountIsConditional;
    }

    public void setDiscountIsConditional(boolean discountIsConditional) {
        this.discountIsConditional = discountIsConditional;
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

    public int getMonthsInCycle() {
        return monthsInCycle;
    }

    public void setMonthsInCycle(int monthsInCycle) {
        this.monthsInCycle = monthsInCycle;
    }

    public String getChargeMode() {
        return chargeMode;
    }

    public void setChargeMode(String chargeMode) {
        this.chargeMode = chargeMode;
    }

    public String getTrialEligibility() {
        return trialEligibility;
    }

    public void setTrialEligibility(String trialEligibility) {
        this.trialEligibility = trialEligibility;
    }

    public int getMaxTrialDays() {
        return maxTrialDays;
    }

    public void setMaxTrialDays(int maxTrialDays) {
        this.maxTrialDays = maxTrialDays;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public String getActivationPath() {
        return activationPath;
    }

    public void setActivationPath(String activationPath) {
        this.activationPath = activationPath;
    }

    public String getBillingEffect() {
        return billingEffect;
    }

    public void setBillingEffect(String billingEffect) {
        this.billingEffect = billingEffect;
    }

    public Long getSucceedsItemId() {
        return succeedsItemId;
    }

    public void setSucceedsItemId(Long succeedsItemId) {
        this.succeedsItemId = succeedsItemId;
    }
}
