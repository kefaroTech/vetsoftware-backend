package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateAnimalColorUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    AnimalColorDto execute(Long id);
}
