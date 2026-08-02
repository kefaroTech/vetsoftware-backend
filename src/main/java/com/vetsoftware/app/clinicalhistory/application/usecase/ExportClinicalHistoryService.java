package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportClinicalEvent;
import com.vetsoftware.app.clinicalhistory.application.port.in.ExportClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalAlertsQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalProblemsQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalReportQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventDetailQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalHistoryPdfPort;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "clinical.history.export")
@Service
public class ExportClinicalHistoryService implements ExportClinicalHistoryUseCase {

  private final ClinicalEventRepository eventRepository;
  private final AnimalReportQueryPort animalQueryPort;
  private final ClinicalEventDetailQueryPort detailQueryPort;
  private final AnimalAlertsQueryPort alertsQueryPort;
  private final AnimalProblemsQueryPort problemsQueryPort;
  private final ClinicalHistoryPdfPort pdfPort;

  public ExportClinicalHistoryService(
      ClinicalEventRepository eventRepository,
      AnimalReportQueryPort animalQueryPort,
      ClinicalEventDetailQueryPort detailQueryPort,
      AnimalAlertsQueryPort alertsQueryPort,
      AnimalProblemsQueryPort problemsQueryPort,
      ClinicalHistoryPdfPort pdfPort) {
    this.eventRepository = eventRepository;
    this.animalQueryPort = animalQueryPort;
    this.detailQueryPort = detailQueryPort;
    this.alertsQueryPort = alertsQueryPort;
    this.problemsQueryPort = problemsQueryPort;
    this.pdfPort = pdfPort;
  }

  @Override
  public byte[] execute(GetClinicalHistoryQuery query) {
    AnimalReportInfo animal =
        animalQueryPort
            .findByIdAndCompanyId(query.animalId(), query.companyId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Animal " + query.animalId() + " not found for current company"));

    List<ReportClinicalEvent> events =
        eventRepository.findHistory(query).stream()
            .map(event -> enrich(event, query.companyId()))
            .toList();

    ClinicalHistoryReportModel model =
        new ClinicalHistoryReportModel(
            animal,
            query.from(),
            query.to(),
            query.types(),
            alertsQueryPort.findByAnimal(query.animalId(), query.companyId()),
            problemsQueryPort.findByAnimal(query.animalId(), query.companyId()),
            events,
            LocalDateTime.now());

    return pdfPort.render(model);
  }

  private ReportClinicalEvent enrich(ClinicalEvent event, Long companyId) {
    ClinicalEventDetail detail =
        detailQueryPort
            .load(event.eventType(), event.sourceId(), companyId)
            .orElseGet(() -> ClinicalEventDetail.of(fallbackTitle(event), List.of()));
    return new ReportClinicalEvent(
        event.sourceId(),
        event.eventType(),
        event.eventDate(),
        event.endDate(),
        event.consultationId(),
        detail.title(),
        detail.fields(),
        detail.tables());
  }

  private static String fallbackTitle(ClinicalEvent event) {
    return event.summary() != null && !event.summary().isBlank()
        ? event.summary()
        : "Sin descripción";
  }
}
