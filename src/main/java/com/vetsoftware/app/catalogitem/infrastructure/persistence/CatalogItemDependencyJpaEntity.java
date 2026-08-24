package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.RelationType;
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
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * {@code catalog_item_dependencies}, ficha 3. Tabla puente, <strong>sin
 * {@code @Version}</strong> ({@code E2_TABLA_PUENTE}): dos claves foráneas y un
 * tipo de relación que se reemplaza borrando e insertando; terna única en base.
 * {@code @SQLDelete} de un solo parámetro.
 *
 * <p>
 * Las dos asociaciones apuntan a la <em>misma</em> feature, y los mappers solo
 * leen su id. Un proxy LAZY sirve el identificador sin ir a la base, así que
 * aquí no hace falta {@code @EntityGraph}: el N+1 que la regla del
 * {@code CLAUDE.md} previene aparece al leer campos del agregado externo, y
 * ninguna consulta de este slice los lee.
 */
@Entity
@Table(name = "catalog_item_dependencies")
@SQLDelete(sql = "UPDATE catalog_item_dependencies SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class CatalogItemDependencyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItemJpaEntity catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_item_id", nullable = false)
    private CatalogItemJpaEntity relatedItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CatalogItemDependencyJpaEntity() {
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

    public CatalogItemJpaEntity getRelatedItem() {
        return relatedItem;
    }

    public void setRelatedItem(CatalogItemJpaEntity relatedItem) {
        this.relatedItem = relatedItem;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
