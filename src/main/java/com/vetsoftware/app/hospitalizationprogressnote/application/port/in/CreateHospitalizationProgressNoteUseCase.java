package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProgressNote.create') or hasRole('SYSTEM')")
    HospitalizationProgressNoteDto execute(CreateHospitalizationProgressNoteCommand command);
}
