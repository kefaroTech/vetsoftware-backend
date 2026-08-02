package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationMedicationsByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
    List<HospitalizationMedicationDto> listByHospitalization(Long hospitalizationId);
}
