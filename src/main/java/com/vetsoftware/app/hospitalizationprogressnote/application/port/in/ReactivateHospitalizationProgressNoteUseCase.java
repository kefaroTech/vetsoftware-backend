package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    HospitalizationProgressNoteDto execute(Long id);
}
