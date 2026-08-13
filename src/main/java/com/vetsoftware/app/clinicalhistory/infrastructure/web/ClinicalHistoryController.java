package com.vetsoftware.app.clinicalhistory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.dto.PageResult;
import com.vetsoftware.app.clinicalhistory.application.port.in.ExportClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.infrastructure.web.response.ClinicalEventResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/animals/{animalId}/clinical-history")
public class ClinicalHistoryController {

    private final GetClinicalHistoryUseCase getUseCase;
    private final ExportClinicalHistoryUseCase exportUseCase;
    private final Authz authz;

    public ClinicalHistoryController(GetClinicalHistoryUseCase getUseCase,
            ExportClinicalHistoryUseCase exportUseCase, Authz authz) {
        this.getUseCase = getUseCase;
        this.exportUseCase = exportUseCase;
        this.authz = authz;
    }

    @GetMapping
    public PageResponse<ClinicalEventResponse> get(@PathVariable Long animalId,
            @RequestParam(name = "types", required = false) List<ClinicalEventType> types,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(animalId,
                authz.currentCompanyId(), types == null ? List.of() : types, from, to, q);
        PageResult<ClinicalEventDto> result = getUseCase.execute(query, page, pageSize);
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> export(@PathVariable Long animalId,
            @RequestParam(name = "types", required = false) List<ClinicalEventType> types,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "q", required = false) String q) {
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(animalId,
                authz.currentCompanyId(), types == null ? List.of() : types, from, to, q);
        byte[] pdf = exportUseCase.execute(query);
        String filename = "historia-clinica-" + animalId + ".pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private ClinicalEventResponse toResponse(ClinicalEventDto dto) {
        return new ClinicalEventResponse(dto.sourceId(), dto.animalId(), dto.eventType(),
                dto.eventDate(), dto.endDate(), dto.consultationId(), dto.summary());
    }
}
