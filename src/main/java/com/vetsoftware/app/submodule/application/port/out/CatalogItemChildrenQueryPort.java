package com.vetsoftware.app.submodule.application.port.out;

/**
 * ¿Hay algun articulo del catalogo comercial que abra este submodulo?
 *
 * <p>
 * Sustituye a {@code MembershipSubModuleChildrenQueryPort}, uno a uno: lo que
 * antes ataba un submodulo a un plan ({@code membership_sub_modules}) ahora lo
 * ata a un articulo vendible ({@code catalog_item_sub_modules}).
 */
public interface CatalogItemChildrenQueryPort {
    boolean existsActiveBySubModuleId(Long subModuleId);
}
