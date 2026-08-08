package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSpasByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('spa.read')")
    PageResult<SpaDto> listByAnimal(Long animalId, String query, int page, int pageSize);
}
