package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Fila de {@code company_capacities}: el techo contratado de una unidad y lo
 * que la empresa lleva usado.
 *
 * <p>
 * <strong>Sin {@code @Version}</strong> ({@code E6_YA_PROTEGIDO}):
 * {@code used_quantity} se mueve con {@code UPDATE ... SET used_quantity =
 * used_quantity + ?}, atomico en el motor. {@code @Version} lo convertiria en
 * un 409 cada vez que dos usuarios se dan de alta a la vez, sin proteger nada
 * que el motor no proteja ya.
 *
 * <p>
 * <strong>Sin {@code enabled}</strong>: es una tabla derivada, misma razon que
 * {@code company_entitlements}.
 */
@Entity
@Table(name = "company_capacities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_company_capacities", columnNames = {"company_id",
                "capacity_unit"})})
public class CompanyCapacityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "capacity_unit", nullable = false, length = 30)
    private String capacityUnit;

    @Column(name = "limit_quantity", nullable = false)
    private int limitQuantity;

    @Column(name = "used_quantity", nullable = false)
    private int usedQuantity;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "recalculated_at", nullable = false)
    private LocalDateTime recalculatedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CompanyCapacityJpaEntity() {
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

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(int limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public void setRecalculatedAt(LocalDateTime recalculatedAt) {
        this.recalculatedAt = recalculatedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
