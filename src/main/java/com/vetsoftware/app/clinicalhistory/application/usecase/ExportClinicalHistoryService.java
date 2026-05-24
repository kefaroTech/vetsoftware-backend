package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.port.in.ExportClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalReportQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalHistoryPdfPort;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "clinicalhistory.export")
@Service
public class ExportClinicalHistoryService implements ExportClinicalHistoryUseCase {

    private final ClinicalEventRepository eventRepository;
    private final AnimalReportQueryPort animalQueryPort;
    private final ClinicalHistoryPdfPort pdfPort;

    public ExportClinicalHistoryService(ClinicalEventRepository eventRepository,
                                        AnimalReportQueryPort animalQueryPort,
                                        ClinicalHistoryPdfPort pdfPort) {
        this.eventRepository = eventRepository;
        this.animalQueryPort = animalQueryPort;
        this.pdfPort = pdfPort;
    }

    @Override
    public byte[] execute(GetClinicalHistoryQuery query) {
        AnimalReportInfo animal = animalQueryPort
                .findByIdAndCompanyId(query.animalId(), query.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal " + query.animalId() + " not found for current company"));

        List<ClinicalEventDto> events = eventRepository.findHistory(query).stream()
                .map(ClinicalEventDto::from)
                .toList();

        ClinicalHistoryReportModel model = new ClinicalHistoryReportModel(
                animal,
                query.from(),
                query.to(),
                query.types(),
                events,
                LocalDateTime.now()
        );

        return pdfPort.render(model);
    }
}
