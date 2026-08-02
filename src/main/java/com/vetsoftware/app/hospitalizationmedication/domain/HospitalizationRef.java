package com.vetsoftware.app.hospitalizationmedication.domain;

import java.time.LocalDate;

public record HospitalizationRef(Long id, LocalDate date) {
  public HospitalizationRef {
    if (id == null) throw new IllegalArgumentException("hospitalization id is required");
    if (date == null) throw new IllegalArgumentException("hospitalization date is required");
  }
}
