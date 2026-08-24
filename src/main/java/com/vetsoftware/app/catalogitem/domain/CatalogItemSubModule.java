package com.vetsoftware.app.catalogitem.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * El puente entre vender y funcionar: qué submódulos abre un artículo.
 *
 * <p>
 * Muchos a muchos a propósito — «Historia clínica» puede abrir consultas,
 * hospitalización y prescripciones de un golpe.
 *
 * <p>
 * {@code catalog_item_id} viaja como {@code Long} pelado y no como companion VO
 * porque apunta a la misma feature; el submódulo, que es de otra, sí va como
 * {@link SubModuleRef}.
 */
public class CatalogItemSubModule {

    private Long id;
    private final Long catalogItemId;
    private final SubModuleRef subModule;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public CatalogItemSubModule(Long id, Long catalogItemId, SubModuleRef subModule,
            LocalDateTime createdDate, boolean enabled) {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (subModule == null)
            throw new IllegalArgumentException("subModule is required");
        this.id = id;
        this.catalogItemId = catalogItemId;
        this.subModule = subModule;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static CatalogItemSubModule create(Long catalogItemId, SubModuleRef subModule,
            Clock clock) {
        return new CatalogItemSubModule(null, catalogItemId, subModule, LocalDateTime.now(clock),
                true);
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

    public SubModuleRef getSubModule() {
        return subModule;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
