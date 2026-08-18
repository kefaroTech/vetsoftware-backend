package com.vetsoftware.app.clinicalhistory.testsupport;

import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailField;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportAlert;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportClinicalEvent;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures del modulo clinicalhistory. Un solo object mother para toda la
 * feature (dominio, queries y DTOs de reporte), como pide la convencion de
 * testing del repo.
 */
public final class ClinicalHistoryMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 77L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long CONSULTATION_SOURCE_ID = 500L;
    public static final LocalDate HOY = LocalDate.of(2026, 8, 12);

    private ClinicalHistoryMother() {
    }

    /** Evento de consulta valido por defecto. */
    public static ClinicalEvent consulta() {
        return consulta(CONSULTATION_SOURCE_ID, HOY);
    }

    public static ClinicalEvent consulta(Long sourceId, LocalDate fecha) {
        return new ClinicalEvent(sourceId, ANIMAL_ID, COMPANY_ID, sourceId, fecha, null,
                ClinicalEventType.CONSULTATION, "Control anual");
    }

    public static ClinicalEvent evento(ClinicalEventType tipo, Long sourceId, LocalDate fecha) {
        return new ClinicalEvent(sourceId, ANIMAL_ID, COMPANY_ID, null, fecha, null, tipo,
                "Resumen " + tipo);
    }

    public static GetClinicalHistoryQuery getHistoryQuery() {
        return new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID, List.of(), null, null, null,
                null);
    }

    public static GetClinicalHistoryQuery getHistoryQuery(Long animalId, Long companyId) {
        return new GetClinicalHistoryQuery(animalId, companyId, List.of(), null, null, null, null);
    }

    public static ListCompanyClinicalEventsQuery listCompanyQuery(LocalDate from, LocalDate to) {
        return new ListCompanyClinicalEventsQuery(COMPANY_ID, List.of(), from, to);
    }

    public static AnimalReportInfo animalReportInfo() {
        return new AnimalReportInfo(ANIMAL_ID, "Firulais", "A-001", "Perro", "Labrador", "Negro",
                "Macho", "Esterilizado", LocalDate.of(2020, 5, 10), "6 años", "12.5 kg",
                LocalDate.of(2026, 8, 1), false, null, "Ana Ruiz", "CC-1020", "3001234567",
                "ana@correo.com", "Calle 1", "Clinica Norte", "NIT-900", "Calle Central",
                "6011234567");
    }

    public static ReportAlert reportAlert() {
        return new ReportAlert("Alergia", "Penicilina", "Alta");
    }

    public static ReportProblem reportProblem() {
        return new ReportProblem("Displasia de cadera", "Activo", true, LocalDate.of(2025, 1, 1),
                null, "Seguimiento trimestral");
    }

    public static ClinicalEventDetail clinicalEventDetail() {
        return ClinicalEventDetail.of("Consulta general",
                List.of(new DetailField("Diagnóstico", "Sano")));
    }

    public static ReportClinicalEvent reportClinicalEvent(ClinicalEventType tipo, LocalDate fecha) {
        return new ReportClinicalEvent(CONSULTATION_SOURCE_ID, tipo, fecha, null, null, "Detalle",
                List.of(new DetailField("Diagnóstico", "Sano")), List.of());
    }

    public static ClinicalHistoryReportModel reportModel(List<ReportClinicalEvent> events) {
        return new ClinicalHistoryReportModel(animalReportInfo(), HOY.minusDays(30), HOY, List.of(),
                List.of(reportAlert()), List.of(reportProblem()), events,
                LocalDateTime.of(2026, 8, 12, 10, 0));
    }
}
