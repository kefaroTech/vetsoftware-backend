package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.read') and @authz.isMyCompany(#companyId))")
    PageResult<SurgeryDto> listByAnimal(Long animalId, Long companyId, String query, int page,
            int pageSize);
}
