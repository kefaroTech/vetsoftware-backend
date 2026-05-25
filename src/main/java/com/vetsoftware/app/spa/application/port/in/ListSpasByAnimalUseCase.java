package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSpasByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('spa.read') or hasRole('SYSTEM')")
    List<SpaDto> listByAnimal(Long animalId);
}
