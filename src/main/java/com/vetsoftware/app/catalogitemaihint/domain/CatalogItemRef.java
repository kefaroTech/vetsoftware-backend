package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * Companion VO del articulo del catalogo al que apunta una pista.
 *
 * <p>
 * {@code catalog_items} vive en la feature {@code catalogitem} y esta feature
 * <strong>no</strong> importa su dominio: guarda esta copia con sus propias
 * invariantes, que es el patron canonico de «Cross-feature references» del
 * {@code CLAUDE.md}. El unico sitio que conoce la otra feature es
 * {@code JpaAiHintCatalogItemQueryPort}. La feature {@code pricelist} tiene su
 * propia copia por el mismo motivo: duplicarlas es la regla, no el descuido.
 *
 * <p>
 * <strong>Es de lectura y solo de lectura.</strong> Ninguna invariante de la
 * pista mira {@code code} ni {@code name} —la pista se identifica por
 * {@code (catalogItemId, hintRevision)}—. Existe por dos motivos concretos: una
 * consola que solo pinta ids es inutilizable, porque el administrador tiene que
 * ver que a {@code GROOMING} le esta diciendo lo que cree; y el camino de
 * escritura necesita saber que el articulo existe antes de que la clave foranea
 * {@code fk_catalog_item_ai_hints_item} lo diga con un 500.
 */
public record CatalogItemRef(Long id, String code, String name) {

    public CatalogItemRef {
        if (id == null)
            throw new IllegalArgumentException("catalog item id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("catalog item code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("catalog item name is required");
    }
}
