package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Fila de {@code company_entitlements}.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong>, y las dos
 * ausencias son decisiones escritas:
 * <ul>
 * <li>{@code E6_YA_PROTEGIDO}: es una tabla derivada que recalcula un unico
 * proceso reescribiendo en bloque los permisos de una empresa dentro de una
 * transaccion. Un 409 aqui bloquearia el recalculo en vez de proteger nada.
 * <li>Sin borrado logico: darla de baja crearia un cuarto estado --ni FULL, ni
 * READ_ONLY, ni NONE, sino invisible-- que nadie sabe interpretar. Por eso no
 * hay {@code @SQLDelete} ni {@code @SQLRestriction} y el repositorio expone un
 * borrado fisico de verdad.
 * </ul>
 *
 * <p>
 * {@code subscription_id} y {@code subscription_item_id} se mapean como
 * columnas planas y no como {@code @ManyToOne}: sus claves foraneas son
 * <strong>compuestas</strong> con {@code company_id}
 * ({@code (company_id, subscription_id) -> subscriptions(company_id, id)}), que
 * es lo que impide que el permiso de una clinica cite el contrato de otra. Esa
 * forma no se expresa con una asociacion sin duplicar la columna de empresa.
 */
@Entity
@Table(name = "company_entitlements", uniqueConstraints = {
        @UniqueConstraint(name = "uq_company_entitlements", columnNames = {"company_id",
                "sub_module_id", "valid_from"})})
public class CompanyEntitlementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_module_id", nullable = false)
    private SubModuleJpaEntity subModule;

    @Column(name = "access_level", nullable = false, length = 15)
    private String accessLevel;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "subscription_item_id")
    private Long subscriptionItemId;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "recalculated_at", nullable = false)
    private LocalDateTime recalculatedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CompanyEntitlementJpaEntity() {
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

    public SubModuleJpaEntity getSubModule() {
        return subModule;
    }

    public void setSubModule(SubModuleJpaEntity subModule) {
        this.subModule = subModule;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
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
