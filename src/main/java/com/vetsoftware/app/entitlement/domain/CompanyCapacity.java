package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDateTime;

/**
 * El techo contratado de una unidad y lo que la empresa lleva usado, para poder
 * avisar antes de bloquear.
 *
 * <p>
 * <strong>No hay invariante {@code used <= limit}, y es deliberado.</strong>
 * Bajar de plan deja legitimamente a un cliente con 5 usuarios y un techo de 3:
 * los datos no se destruyen, se le impide crear mas. Una regla que lo
 * prohibiera haria imposible registrar la bajada, que es una operacion normal.
 *
 * <p>
 * {@code used_quantity} no se mueve nunca leyendo, modificando y guardando
 * desde Java: va con un
 * {@code UPDATE ... SET used_quantity = used_quantity + ?} atomico en el motor.
 * Por eso la tabla no lleva {@code version} ({@code E6_YA_PROTEGIDO}): un 409
 * cada vez que dos usuarios se dan de alta a la vez no protegeria nada.
 */
public class CompanyCapacity {

    private final Long id;
    private final Long companyId;
    private final CapacityUnit unit;
    private final int limitQuantity;
    private final int usedQuantity;
    private final Long subscriptionId;
    private final LocalDateTime recalculatedAt;
    private final LocalDateTime createdDate;

    public CompanyCapacity(Long id, Long companyId, CapacityUnit unit, int limitQuantity,
            int usedQuantity, Long subscriptionId, LocalDateTime recalculatedAt,
            LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (unit == null)
            throw new IllegalArgumentException("capacity unit is required");
        if (recalculatedAt == null)
            throw new IllegalArgumentException("recalculated at is required");
        // chk_company_capacities_quantities
        if (limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (usedQuantity < 0)
            throw new IllegalArgumentException("used quantity cannot be negative");
        this.id = id;
        this.companyId = companyId;
        this.unit = unit;
        this.limitQuantity = limitQuantity;
        this.usedQuantity = usedQuantity;
        this.subscriptionId = subscriptionId;
        this.recalculatedAt = recalculatedAt;
        this.createdDate = createdDate == null ? recalculatedAt : createdDate;
    }

    /** Contador recien derivado del contrato: sin id y sin consumo previo. */
    public static CompanyCapacity contracted(Long companyId, CapacityUnit unit, int limitQuantity,
            Long subscriptionId, LocalDateTime recalculatedAt) {
        return new CompanyCapacity(null, companyId, unit, limitQuantity, 0, subscriptionId,
                recalculatedAt, recalculatedAt);
    }

    /**
     * El techo nuevo sobre el contador que ya existia. Conserva id, consumo y fecha
     * de creacion: <strong>el recalculo no puede perder
     * {@code used_quantity}</strong>, que es un dato del mundo real y no una
     * derivacion del contrato.
     */
    public CompanyCapacity reconciledFrom(CompanyCapacity existing) {
        if (existing == null)
            return this;
        return new CompanyCapacity(existing.id, companyId, unit, limitQuantity,
                existing.usedQuantity, subscriptionId, recalculatedAt, existing.createdDate);
    }

    /**
     * El techo cae a cero: la unidad dejo de estar contratada, el consumo se queda.
     */
    public CompanyCapacity withoutContract(Long noSubscriptionId, LocalDateTime at) {
        return new CompanyCapacity(id, companyId, unit, 0, usedQuantity, noSubscriptionId, at,
                createdDate);
    }

    /** Ya no queda margen: el momento exacto de ofrecer la ampliacion. */
    public boolean isExhausted() {
        return usedQuantity >= limitQuantity;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public CapacityUnit getUnit() {
        return unit;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
