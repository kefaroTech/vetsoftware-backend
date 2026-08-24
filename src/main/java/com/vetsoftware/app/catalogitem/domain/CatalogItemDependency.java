package com.vetsoftware.app.catalogitem.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Una regla del configurador entre dos artículos.
 *
 * <p>
 * Sin esta tabla, un cliente compra «Facturación electrónica» sin «Caja» y
 * descubre después de pagar que no le sirve.
 *
 * <p>
 * El ciclo trivial ({@code A → A}) lo cierra aquí la invariante del constructor
 * y en la base {@code chk_catalog_item_dependencies_not_self}. El
 * <strong>indirecto</strong> ({@code A → B → C → A}) no es expresable en MySQL
 * y lo cierra {@link DependencyGraph} desde el caso de uso — regla R16.
 */
public class CatalogItemDependency {

    private Long id;
    private final Long catalogItemId;
    private final Long relatedItemId;
    private RelationType relationType;
    private String note;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public CatalogItemDependency(Long id, Long catalogItemId, Long relatedItemId,
            RelationType relationType, String note, LocalDateTime createdDate, boolean enabled) {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (relatedItemId == null)
            throw new IllegalArgumentException("relatedItemId is required");
        if (catalogItemId.equals(relatedItemId))
            throw new IllegalArgumentException(
                    "a catalog item cannot depend on itself: " + catalogItemId);
        validateRelacion(relationType, note);
        this.id = id;
        this.catalogItemId = catalogItemId;
        this.relatedItemId = relatedItemId;
        this.relationType = relationType;
        this.note = note;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static CatalogItemDependency create(Long catalogItemId, Long relatedItemId,
            RelationType relationType, String note, Clock clock) {
        return new CatalogItemDependency(null, catalogItemId, relatedItemId, relationType, note,
                LocalDateTime.now(clock), true);
    }

    /**
     * El par de artículos no se reapunta: eso es otra dependencia. Lo que se edita
     * es el sentido de la regla y el mensaje que lee el cliente.
     */
    public void update(RelationType relationType, String note) {
        validateRelacion(relationType, note);
        this.relationType = relationType;
        this.note = note;
    }

    private static void validateRelacion(RelationType relationType, String note) {
        if (relationType == null)
            throw new IllegalArgumentException("relationType is required");
        if (note != null && note.length() > 255)
            throw new IllegalArgumentException("note must be 255 chars or less");
    }

    /** Solo {@code REQUIRES} arrastra, y por tanto solo él puede formar ciclos. */
    public boolean arrastra() {
        return relationType == RelationType.REQUIRES;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public Long getRelatedItemId() {
        return relatedItemId;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
