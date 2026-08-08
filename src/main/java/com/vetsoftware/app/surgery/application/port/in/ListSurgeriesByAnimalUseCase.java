package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('surgery.read')")
    PageResult<SurgeryDto> listByAnimal(Long animalId, int page, int pageSize);
}
