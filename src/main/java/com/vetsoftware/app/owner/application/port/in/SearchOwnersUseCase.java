package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchOwnersUseCase {

    /**
     * La búsqueda se pagina igual que el listado: filtrar en servidor no acota nada
     * por sí solo —un término de una letra devuelve casi toda la tabla— y el front
     * necesita las mismas páginas para el scroll.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('owner.read') and @authz.isMyCompany(#companyId))")
    PageResult<OwnerDto> search(Long companyId, String query, int page, int pageSize);
}
