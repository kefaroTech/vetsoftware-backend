package com.vetsoftware.app.hospitalizationprogressnote.application.port.in;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationProgressNotesByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProgressNote.read') or hasRole('SYSTEM')")
    List<HospitalizationProgressNoteDto> listByHospitalization(Long hospitalizationId);
}
