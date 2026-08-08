package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationMedicationsByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
    PageResult<HospitalizationMedicationDto> listByHospitalization(Long hospitalizationId, int page,
            int pageSize);
}
