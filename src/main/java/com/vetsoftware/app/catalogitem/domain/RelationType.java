package com.vetsoftware.app.catalogitem.domain;

/**
 * Qué dice una dependencia del configurador sobre el par de artículos.
 *
 * <p>
 * Espejo de {@code chk_catalog_item_dependencies_type}. Solo {@link #REQUIRES}
 * arrastra, y por eso es el único arco que recorre {@link DependencyGraph}: un
 * «ciclo» de recomendaciones es inofensivo y una exclusión no encadena nada.
 */
public enum RelationType {
    /** Sin el otro artículo, este no sirve. Arrastra, y por eso puede ciclar. */
    REQUIRES,
    /** Sugerencia comercial. No arrastra nada. */
    RECOMMENDS,
    /** Los dos artículos no pueden coexistir en la misma cotización. */
    EXCLUDES
}
