package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgery.read') or hasRole('SYSTEM')")
    List<SurgeryDto> listByAnimal(Long animalId);
}
