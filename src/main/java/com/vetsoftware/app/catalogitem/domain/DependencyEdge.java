package com.vetsoftware.app.catalogitem.domain;

/**
 * Un arco {@code REQUIRES} del grafo de dependencias, sin nada más.
 *
 * <p>
 * Existe para que {@link DependencyGraph} no dependa de
 * {@link CatalogItemDependency} entera: al detector de ciclos solo le hacen
 * falta los dos extremos, y proyectar solo eso es lo que permite que el
 * adaptador cargue el grafo con una consulta de dos columnas en vez de hidratar
 * la tabla con sus dos {@code @ManyToOne}.
 */
public record DependencyEdge(Long catalogItemId, Long relatedItemId) {

    public DependencyEdge {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (relatedItemId == null)
            throw new IllegalArgumentException("relatedItemId is required");
    }
}
