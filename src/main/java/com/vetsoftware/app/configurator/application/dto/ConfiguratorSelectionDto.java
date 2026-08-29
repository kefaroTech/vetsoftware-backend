package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import com.vetsoftware.app.configurator.domain.SelectedItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lo que el configurador mete en el carrito con unas respuestas dadas.
 *
 * <p>
 * Es el contrato que consumira {@code quote}: a partir de aqui, quien cotiza
 * resuelve precios y congela las lineas. El configurador no sabe de dinero.
 *
 * <p>
 * <strong>Sale por rotulo y no por id</strong> —ver {@link SelectedItemDto}—,
 * que es lo que permite pasar esta lista directamente a
 * {@code POST /quotes/self-serve} y tarifarla contra {@code GET /catalog}.
 */
public record ConfiguratorSelectionDto(List<SelectedItemDto> items) {

    public ConfiguratorSelectionDto {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Traduce los ids que resolvio el dominio a los rotulos publicos.
     *
     * <p>
     * <strong>Un articulo sin rotulo se descarta, no se publica a medias.</strong>
     * Ocurre cuando un efecto apunta a un articulo que se retiro de la venta
     * despues de sembrarlo: no esta {@code ACTIVE}, asi que
     * {@code CatalogItemQueryPort} no lo devuelve. Meterlo en el carrito con un
     * hueco donde va el codigo daria una linea que el front no puede pintar y que
     * la contratacion rechazaria despues; dejarlo fuera es lo que ya hace el resto
     * de la superficie publica con lo que no esta activo.
     */
    public static ConfiguratorSelectionDto from(List<SelectedItem> items,
            List<CatalogItemRef> refs) {
        Map<Long, String> porId = refs == null
                ? Map.of()
                : refs.stream().collect(Collectors.toMap(CatalogItemRef::id, CatalogItemRef::code));
        return new ConfiguratorSelectionDto(items.stream()
                .filter(item -> porId.containsKey(item.catalogItemId()))
                .map(item -> new SelectedItemDto(porId.get(item.catalogItemId()), item.quantity()))
                .toList());
    }
}
