package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAnimalsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read') and @authz.isMyCompany(#companyId))")
    PageResult<AnimalDto> listAll(Long companyId, int page, int pageSize);
}
