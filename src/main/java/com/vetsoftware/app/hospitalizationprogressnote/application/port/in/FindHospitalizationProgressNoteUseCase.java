package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProgressNote.read') or hasRole('SYSTEM')")
    HospitalizationProgressNoteDto findById(Long id);
}
