package com.vetsoftware.app.clinicalhistory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.ListCompanyClinicalEventsUseCase;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.infrastructure.web.response.ClinicalEventResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clinical-history")
public class ClinicalHistoryCompanyController {

  private final ListCompanyClinicalEventsUseCase listByCompanyUseCase;
  private final Authz authz;

  public ClinicalHistoryCompanyController(
      ListCompanyClinicalEventsUseCase listByCompanyUseCase, Authz authz) {
    this.listByCompanyUseCase = listByCompanyUseCase;
    this.authz = authz;
  }

  @GetMapping
  public List<ClinicalEventResponse> listByCompany(
      @RequestParam(name = "types", required = false) List<ClinicalEventType> types,
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    ListCompanyClinicalEventsQuery query =
        new ListCompanyClinicalEventsQuery(
            authz.currentCompanyId(), types == null ? List.of() : types, from, to);
    return listByCompanyUseCase.execute(query).stream().map(this::toResponse).toList();
  }

  private ClinicalEventResponse toResponse(ClinicalEventDto dto) {
    return new ClinicalEventResponse(
        dto.sourceId(),
        dto.animalId(),
        dto.eventType(),
        dto.eventDate(),
        dto.endDate(),
        dto.consultationId(),
        dto.summary());
  }
}
