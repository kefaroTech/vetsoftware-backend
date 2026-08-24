package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDateTime;

/**
 * Que puede usar una empresa de un submodulo, ahora mismo.
 *
 * <p>
 * <strong>No contiene ninguna decision.</strong> Es el resultado de aplicar el
 * contrato vigente, y si se corrompe se recalcula entera desde los contratos,
 * que son la verdad. Por eso la tabla no lleva {@code version}
 * ({@code E6_YA_PROTEGIDO}) ni {@code enabled}: darla de baja logicamente
 * crearia un cuarto estado --ni FULL, ni READ_ONLY, ni NONE, sino invisible--
 * que nadie sabe interpretar.
 *
 * <p>
 * Las invariantes del constructor son el espejo exacto de los {@code CHECK} de
 * la tabla, para que un permiso invalido muera en el dominio y no en un error
 * de motor a mitad de una transaccion de recalculo.
 */
public class CompanyEntitlement {

    private final Long id;
    private final Long companyId;
    private final SubModuleRef subModule;
    private final AccessLevel accessLevel;
    private final EntitlementSource source;
    private final Long subscriptionId;
    private final Long subscriptionItemId;
    private final LocalDateTime validFrom;
    private final LocalDateTime validUntil;
    private final LocalDateTime recalculatedAt;
    private final LocalDateTime createdDate;

    public CompanyEntitlement(Long id, Long companyId, SubModuleRef subModule,
            AccessLevel accessLevel, EntitlementSource source, Long subscriptionId,
            Long subscriptionItemId, LocalDateTime validFrom, LocalDateTime validUntil,
            LocalDateTime recalculatedAt, LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (subModule == null)
            throw new IllegalArgumentException("sub module is required");
        if (accessLevel == null)
            throw new IllegalArgumentException("access level is required");
        if (source == null)
            throw new IllegalArgumentException("entitlement source is required");
        if (validFrom == null)
            throw new IllegalArgumentException("valid from is required");
        if (recalculatedAt == null)
            throw new IllegalArgumentException("recalculated at is required");
        // chk_company_entitlements_validity
        if (validUntil != null && !validUntil.isAfter(validFrom))
            throw new IllegalArgumentException("valid until must be after valid from");
        // chk_company_entitlements_origin
        if (source.requiresSubscription() && subscriptionId == null)
            throw new IllegalArgumentException(
                    "subscription id is required for source " + source.name());
        this.id = id;
        this.companyId = companyId;
        this.subModule = subModule;
        this.accessLevel = accessLevel;
        this.source = source;
        this.subscriptionId = subscriptionId;
        this.subscriptionItemId = subscriptionItemId;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.recalculatedAt = recalculatedAt;
        this.createdDate = createdDate == null ? recalculatedAt : createdDate;
    }

    /**
     * Permiso recien derivado del contrato: sin id, porque el recalculo borra
     * fisicamente las filas de la empresa y las reinserta.
     */
    public static CompanyEntitlement derived(Long companyId, SubModuleRef subModule,
            AccessLevel accessLevel, EntitlementSource source, Long subscriptionId,
            Long subscriptionItemId, LocalDateTime validFrom, LocalDateTime validUntil,
            LocalDateTime recalculatedAt) {
        return new CompanyEntitlement(null, companyId, subModule, accessLevel, source,
                subscriptionId, subscriptionItemId, validFrom, validUntil, recalculatedAt,
                recalculatedAt);
    }

    /**
     * La ventana esta abierta en ese instante. Es lo que hace que la prueba caduque
     * sola a la fecha, sin ningun proceso que se pueda olvidar de correr.
     */
    public boolean isActiveAt(LocalDateTime at) {
        return !validFrom.isAfter(at) && (validUntil == null || validUntil.isAfter(at));
    }

    /** Ventana abierta y nivel que deja ver algo. */
    public boolean grantsAt(LocalDateTime at) {
        return isActiveAt(at) && accessLevel.allowsRead();
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public SubModuleRef getSubModule() {
        return subModule;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public EntitlementSource getSource() {
        return source;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public Long getSubscriptionItemId() {
        return subscriptionItemId;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
