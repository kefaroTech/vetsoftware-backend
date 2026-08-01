package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDewormingsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('deworming.read')")
    List<DewormingDto> listByAnimal(Long animalId);
}
