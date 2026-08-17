package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProgressNotesByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    PageResult<HospitalizationProgressNoteDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize);
}
