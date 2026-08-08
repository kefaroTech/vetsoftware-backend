package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDayCaresByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('dayCare.read')")
    PageResult<DayCareDto> listByAnimal(Long animalId, int page, int pageSize);
}
