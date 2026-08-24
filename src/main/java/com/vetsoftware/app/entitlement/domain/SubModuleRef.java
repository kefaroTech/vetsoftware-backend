package com.vetsoftware.app.entitlement.domain;

/**
 * Companion VO del submodulo al que apunta un permiso. Es la unica forma en que
 * este slice conoce a {@code submodule}: nunca su entidad de dominio.
 */
public record SubModuleRef(Long id, String code, String name) {
    public SubModuleRef {
        if (id == null)
            throw new IllegalArgumentException("sub module id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("sub module code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("sub module name is required");
    }
}
