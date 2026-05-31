package com.vetsoftware.app.hospitalizationprogressnote.application.dto;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import java.time.LocalDateTime;

public record HospitalizationProgressNoteDto(
        Long id,
        String description,
        HospitalizationSummaryDto hospitalization,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static HospitalizationProgressNoteDto from(HospitalizationProgressNote progressNote) {
        return new HospitalizationProgressNoteDto(
            progressNote.getId(),
            progressNote.getDescription(),
            HospitalizationSummaryDto.from(progressNote.getHospitalization()),
            EmployeeSummaryDto.from(progressNote.getCreatedBy()),
            progressNote.getCreatedDate(),
            progressNote.isEnabled());
    }
}
