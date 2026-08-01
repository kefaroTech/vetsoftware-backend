package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('hospitalization.read')) and @authz.isMyCompany(#companyId))")
    HospitalizationProgressNoteDto findById(Long id, Long companyId);
}
