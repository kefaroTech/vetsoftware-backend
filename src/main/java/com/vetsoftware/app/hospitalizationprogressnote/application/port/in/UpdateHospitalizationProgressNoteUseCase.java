package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.UpdateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationProgressNoteUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
    HospitalizationProgressNoteDto execute(UpdateHospitalizationProgressNoteCommand command);
}
