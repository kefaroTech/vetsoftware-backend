package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    HospitalizationDto execute(UpdateHospitalizationCommand command);
}
