package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('surgery.read')")
    List<SurgeryDto> listByAnimal(Long animalId);
}
