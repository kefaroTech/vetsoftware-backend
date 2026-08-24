package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
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
 * {@code catalog_item_sub_modules}, ficha 2. Tabla puente pura.
 *
 * <p>
 * <strong>Sin {@code @Version}</strong>, con código de exención
 * {@code E2_TABLA_PUENTE}: solo dos claves foráneas, ningún campo propio
 * mutable, par único en base. Por eso —y solo por eso— su {@code @SQLDelete}
 * lleva <em>un</em> parámetro y no dos.
 *
 * <p>
 * El {@code @ManyToOne} contra {@code SubModuleJpaEntity} es el único cruce de
 * vertical slicing que el {@code CLAUDE.md} permite, y vive donde tiene que
 * vivir: en {@code infrastructure/persistence}.
 */
@Entity
@Table(name = "catalog_item_sub_modules")
@SQLDelete(sql = "UPDATE catalog_item_sub_modules SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class CatalogItemSubModuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItemJpaEntity catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_module_id", nullable = false)
    private SubModuleJpaEntity subModule;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CatalogItemSubModuleJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CatalogItemJpaEntity getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(CatalogItemJpaEntity catalogItem) {
        this.catalogItem = catalogItem;
    }

    public SubModuleJpaEntity getSubModule() {
        return subModule;
    }

    public void setSubModule(SubModuleJpaEntity subModule) {
        this.subModule = subModule;
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
