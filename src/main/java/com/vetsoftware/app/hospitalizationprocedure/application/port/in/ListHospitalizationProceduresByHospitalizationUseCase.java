package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProceduresByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    PageResult<HospitalizationProcedureDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize);
}
