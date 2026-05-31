package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProceduresByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProcedure.read') or hasRole('SYSTEM')")
    List<HospitalizationProcedureDto> listByHospitalization(Long hospitalizationId);
}
