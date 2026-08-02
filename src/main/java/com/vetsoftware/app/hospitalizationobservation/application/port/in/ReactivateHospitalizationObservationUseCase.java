package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationObservationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
  HospitalizationObservationDto execute(Long id);
}
