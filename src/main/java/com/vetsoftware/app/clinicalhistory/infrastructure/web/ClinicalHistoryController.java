package com.vetsoftware.app.clinicalhistory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.infrastructure.web.response.ClinicalEventResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/animals/{animalId}/clinical-history")
public class ClinicalHistoryController {

    private final GetClinicalHistoryUseCase getUseCase;
    private final Authz authz;

    public ClinicalHistoryController(GetClinicalHistoryUseCase getUseCase, Authz authz) {
        this.getUseCase = getUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<ClinicalEventResponse> get(
            @PathVariable Long animalId,
            @RequestParam(name = "types", required = false) List<ClinicalEventType> types,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(
                animalId,
                authz.currentCompanyId(),
                types == null ? List.of() : types,
                from,
                to
        );
        return getUseCase.execute(query).stream().map(this::toResponse).toList();
    }

    private ClinicalEventResponse toResponse(ClinicalEventDto dto) {
        return new ClinicalEventResponse(
                dto.sourceId(),
                dto.eventType(),
                dto.eventDate(),
                dto.consultationId(),
                dto.summary()
        );
    }
}
