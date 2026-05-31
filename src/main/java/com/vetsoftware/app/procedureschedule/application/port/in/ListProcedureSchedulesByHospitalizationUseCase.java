package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProcedureSchedulesByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.read') or hasRole('SYSTEM')")
    List<ProcedureScheduleDto> listByHospitalization(Long hospitalizationId);
}
