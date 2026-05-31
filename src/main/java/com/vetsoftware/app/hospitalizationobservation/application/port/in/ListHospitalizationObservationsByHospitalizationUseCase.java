package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationObservationsByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationObservation.read') or hasRole('SYSTEM')")
    List<HospitalizationObservationDto> listByHospitalization(Long hospitalizationId);
}
