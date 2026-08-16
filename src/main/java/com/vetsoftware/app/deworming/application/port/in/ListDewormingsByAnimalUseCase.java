package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDewormingsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('deworming.read') and @authz.isMyCompany(#companyId))")
    PageResult<DewormingDto> listByAnimal(Long animalId, Long companyId, String query, int page,
            int pageSize);
}
