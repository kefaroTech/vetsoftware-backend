package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOwnersUseCase {

    /**
     * Paginado, nunca la lista entera. Los propietarios crecen con cada cliente de
     * la veterinaria: una consulta sin techo aquí es la que tumba la memoria del
     * backend cuando la empresa lleva años operando.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('owner.read') and @authz.isMyCompany(#companyId))")
    PageResult<OwnerDto> listAll(Long companyId, int page, int pageSize);
}
