package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOwnersUseCase {

    /**
     * BE-06: devolvía la lista entera. Los propietarios crecen con cada cliente de
     * la veterinaria, así que era una consulta sin techo — la que tumba la memoria
     * del backend cuando la empresa lleva años operando.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('owner.read') and @authz.isMyCompany(#companyId))")
    PageResult<OwnerDto> listAll(Long companyId, int page, int pageSize);
}
