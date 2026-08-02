package com.vetsoftware.app.clinicalhistory.infrastructure.pdf;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportClinicalEvent;
import java.util.List;

/**
 * View-model para el template PDF: agrupado de eventos por mes con label en
 * español.
 */
public record ClinicalHistoryMonthGroup(String key, String label,
        List<ReportClinicalEvent> events) {
}
