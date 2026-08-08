package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProceduresByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
    PageResult<HospitalizationProcedureDto> listByHospitalization(Long hospitalizationId, int page,
            int pageSize);
}
