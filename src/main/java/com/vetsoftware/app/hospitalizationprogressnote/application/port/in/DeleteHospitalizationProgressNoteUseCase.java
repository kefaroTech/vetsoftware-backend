package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProgressNote.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
