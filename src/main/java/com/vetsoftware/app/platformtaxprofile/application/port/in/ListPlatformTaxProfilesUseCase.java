package com.vetsoftware.app.platformtaxprofile.application.port.in;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** El historico de identidades fiscales, de la vigente a la mas antigua. */
public interface ListPlatformTaxProfilesUseCase {

    /**
     * <strong>Se llama {@code listAll} y no {@code listByCompany} porque no hay
     * empresa por la que filtrar</strong>, y por eso mismo el unico gate posible es
     * {@code hasRole('SYSTEM')} a secas: {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}
     * (dura, BE-29) examina si el puerto transporta un {@code companyId} antes de
     * mirar nada mas.
     *
     * <p>
     * Aqui la regla y el modelo dicen lo mismo, que no siempre pasa: no existe la
     * variante acotada por empresa ni podria existir, porque
     * {@code platform_tax_profiles} no tiene la columna. Añadir un
     * {@code companyId} a esta firma para «poder abrirla al tenant» seria inventar
     * un filtro que no filtra.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PlatformTaxProfileDto> listAll(int page, int pageSize);
}
