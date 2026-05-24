package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDayCaresByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('dayCare.read') or hasRole('SYSTEM')")
    List<DayCareDto> listByAnimal(Long animalId);
}
