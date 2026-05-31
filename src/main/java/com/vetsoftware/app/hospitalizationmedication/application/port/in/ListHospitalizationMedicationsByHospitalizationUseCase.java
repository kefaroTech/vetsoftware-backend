package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationMedicationsByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationMedication.read') or hasRole('SYSTEM')")
    List<HospitalizationMedicationDto> listByHospitalization(Long hospitalizationId);
}
