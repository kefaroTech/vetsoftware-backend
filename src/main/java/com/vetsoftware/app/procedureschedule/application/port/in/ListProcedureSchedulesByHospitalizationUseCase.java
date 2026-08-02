package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProcedureSchedulesByHospitalizationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
  List<ProcedureScheduleDto> listByHospitalization(Long hospitalizationId);
}
