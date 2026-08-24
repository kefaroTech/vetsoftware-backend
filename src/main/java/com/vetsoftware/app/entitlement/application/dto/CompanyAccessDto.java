package com.vetsoftware.app.entitlement.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que una empresa puede usar y consumir ahora mismo: la respuesta de la
 * consulta caliente, la que se hace al pintar el menu.
 *
 * <p>
 * Solo trae permisos con la ventana abierta y nivel que deje ver algo; los
 * {@code NONE} y los caducados no viajan, porque para la interfaz "oculto" y
 * "no existe" son lo mismo.
 *
 * <p>
 * {@code recalculatedAt} es el mas antiguo de la empresa y va aqui a proposito:
 * si se queda viejo hay un proceso caido, y es la unica forma de que quien
 * consume estos permisos pueda notarlo.
 */
public record CompanyAccessDto(Long companyId, List<CompanyEntitlementDto> entitlements,
        List<CompanyCapacityDto> capacities, LocalDateTime recalculatedAt) {

    public CompanyAccessDto {
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
        capacities = capacities == null ? List.of() : List.copyOf(capacities);
    }
}
