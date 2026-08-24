package com.vetsoftware.app.catalogitem.domain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Regla R16: las dependencias entre articulos no pueden formar ciclos
 * indirectos.
 *
 * <p>
 * <strong>Que se rompe si entra un ciclo:</strong> el configurador resuelve
 * dependencias en bucle y no se puede cotizar. El ciclo directo lo cierra
 * {@code chk_catalog_item_dependencies_not_self}; el indirecto no es expresable
 * en un {@code CHECK} de MySQL y lo cierra {@link DependencyGraph}.
 *
 * <p>
 * El mensaje lleva la ruta completa a proposito: decirle al administrador del
 * catalogo «hay un ciclo» le obliga a buscarlo a mano entre decenas de arcos.
 */
public class CatalogItemDependencyCycleException extends RuntimeException {

    private final transient List<Long> cycle;

    public CatalogItemDependencyCycleException(List<Long> cycle) {
        super("REQUIRES dependency would close a cycle: "
                + cycle.stream().map(String::valueOf).collect(Collectors.joining(" > ")));
        this.cycle = List.copyOf(cycle);
    }

    /** La ruta que cierra el bucle, del articulo sujeto a el mismo. */
    public List<Long> getCycle() {
        return cycle;
    }
}
