package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationProgressNoteUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.create')")
  HospitalizationProgressNoteDto execute(CreateHospitalizationProgressNoteCommand command);
}
