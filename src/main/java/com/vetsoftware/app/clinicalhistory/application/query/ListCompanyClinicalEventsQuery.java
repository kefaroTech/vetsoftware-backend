package com.vetsoftware.app.clinicalhistory.application.query;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;

public record ListCompanyClinicalEventsQuery(
    Long companyId, List<ClinicalEventType> types, LocalDate from, LocalDate to) {
  public ListCompanyClinicalEventsQuery {
    if (companyId == null) throw new IllegalArgumentException("companyId is required");
    if (types == null) types = List.of();
    if (from != null && to != null && to.isBefore(from)) {
      throw new IllegalArgumentException("'to' cannot be before 'from'");
    }
  }
}
