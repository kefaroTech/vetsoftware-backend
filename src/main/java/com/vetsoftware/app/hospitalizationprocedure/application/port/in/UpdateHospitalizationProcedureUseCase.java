package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationProcedureUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
    HospitalizationProcedureDto execute(UpdateHospitalizationProcedureCommand command);
}
