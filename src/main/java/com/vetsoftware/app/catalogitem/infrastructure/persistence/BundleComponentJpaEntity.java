package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * {@code bundle_components}, ficha 4. Tabla puente, <strong>sin
 * {@code @Version}</strong> ({@code E2_TABLA_PUENTE}): la composición de un
 * paquete se reescribe en bloque desde su editor y el par es único en base.
 * {@code @SQLDelete} de un solo parámetro.
 */
@Entity
@Table(name = "bundle_components")
@SQLDelete(sql = "UPDATE bundle_components SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class BundleComponentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_item_id", nullable = false)
    private CatalogItemJpaEntity bundleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_item_id", nullable = false)
    private CatalogItemJpaEntity componentItem;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected BundleComponentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CatalogItemJpaEntity getBundleItem() {
        return bundleItem;
    }

    public void setBundleItem(CatalogItemJpaEntity bundleItem) {
        this.bundleItem = bundleItem;
    }

    public CatalogItemJpaEntity getComponentItem() {
        return componentItem;
    }

    public void setComponentItem(CatalogItemJpaEntity componentItem) {
        this.componentItem = componentItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
