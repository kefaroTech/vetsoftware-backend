package com.vetsoftware.app.catalogitem.application.dto;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import java.time.LocalDateTime;

/**
 * @param defaultTrialDays
 *            los dias de prueba que el articulo concede, o {@code null} si no
 *            concede ninguna. Existia en la tabla desde el changeset 229 y no
 *            llegaba a ningun API: la pantalla que promete «30 dias gratis en
 *            Agenda» no tenia de donde leer el numero y acababa escribiendolo a
 *            mano en el front, que es como una promesa comercial se separa del
 *            catalogo sin que nadie se entere.
 */
public record CatalogItemDto(Long id, String code, String name, String shortDescription,
        String longDescription, ItemType itemType, String capacityUnit, boolean core,
        int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status,
        LocalDateTime createdDate, boolean enabled, Integer defaultTrialDays) {

    /**
     * Sin politica de prueba. Conveniencia para quien describe un articulo que no
     * la tiene, en la misma linea que la sobrecarga de {@code QuoteLineCommand}.
     */
    public CatalogItemDto(Long id, String code, String name, String shortDescription,
            String longDescription, ItemType itemType, String capacityUnit, boolean core,
            int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status,
            LocalDateTime createdDate, boolean enabled) {
        this(id, code, name, shortDescription, longDescription, itemType, capacityUnit, core,
                minQuantity, maxQuantity, sortOrder, status, createdDate, enabled, null);
    }

    public static CatalogItemDto from(CatalogItem item) {
        return new CatalogItemDto(item.getId(), item.getCode(), item.getName(),
                item.getShortDescription(), item.getLongDescription(), item.getItemType(),
                item.getCapacityUnit(), item.isCore(), item.getMinQuantity(), item.getMaxQuantity(),
                item.getSortOrder(), item.getStatus(), item.getCreatedDate(), item.isEnabled(),
                item.getDefaultTrialDays());
    }
}
