package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Por qué todos los puertos de este slice son {@code hasRole("SYSTEM")}
 * a secas.</strong> Las cuatro tablas del catálogo comercial
 * ({@code catalog_items}, {@code catalog_item_sub_modules},
 * {@code catalog_item_dependencies} y {@code bundle_components}) no llevan
 * {@code company_id}: son el estante de la tienda, global de plataforma. No hay
 * empresa que derivar del principal ni ownership de fila que comprobar, así que
 * ni {@code @authz.isMyCompany} ni una variante acotada tendrían nada que
 * validar. {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) se satisface por
 * aquí, y las cuatro reglas de la familia BE-COV ni miran este slice porque
 * ninguna de sus entidades JPA alcanza {@code CompanyJpaEntity}.
 */
public interface CreateCatalogItemUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDto execute(CreateCatalogItemCommand command);
}
