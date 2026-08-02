package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListWeightRecordsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read'))")
    List<WeightRecordDto> listByAnimal(Long animalId, Long companyId);
}
