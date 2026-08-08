package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProgressNotesByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
    PageResult<HospitalizationProgressNoteDto> listByHospitalization(Long hospitalizationId,
            int page, int pageSize);
}
