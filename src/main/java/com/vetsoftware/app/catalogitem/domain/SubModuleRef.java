package com.vetsoftware.app.catalogitem.domain;

/**
 * Companion VO del submódulo que abre un artículo del catálogo.
 *
 * <p>
 * {@code sub_modules} vive en la feature {@code submodule} y esta feature
 * <strong>no</strong> importa su dominio: guarda esta copia con sus propias
 * invariantes, que es el patrón canónico de «Cross-feature references» del
 * {@code CLAUDE.md}. El único sitio que conoce la otra feature es
 * {@code JpaSubModuleQueryPort}.
 *
 * <p>
 * Se queda deliberadamente en {@code (id, name, code)} y no arrastra
 * {@code is_sellable} ni {@code read_only_capable}: esas dos columnas las está
 * añadiendo el paso 0.1 de la especificación en otro slice, y depender de ellas
 * antes de que existan rompería este.
 */
public record SubModuleRef(Long id, String name, String code) {

    public SubModuleRef {
        if (id == null)
            throw new IllegalArgumentException("sub module id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("sub module name is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("sub module code is required");
    }
}
