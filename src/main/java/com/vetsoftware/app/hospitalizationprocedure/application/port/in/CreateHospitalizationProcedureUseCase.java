package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationProcedureUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProcedure.create') or hasRole('SYSTEM')")
    HospitalizationProcedureDto execute(CreateHospitalizationProcedureCommand command);
}
