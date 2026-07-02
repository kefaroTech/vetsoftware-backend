package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLatestWeightRecordUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    WeightRecordDto findLatest(Long animalId, Long companyId);
}
