package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.create')")
    HospitalizationDto execute(CreateHospitalizationCommand command);
}
