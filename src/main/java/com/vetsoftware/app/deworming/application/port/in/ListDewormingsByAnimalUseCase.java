package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDewormingsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('deworming.read')")
    PageResult<DewormingDto> listByAnimal(Long animalId, int page, int pageSize);
}
