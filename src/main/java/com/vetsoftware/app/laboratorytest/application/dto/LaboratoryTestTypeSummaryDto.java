package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;

public record LaboratoryTestTypeSummaryDto(Long id, String name) {
  public static LaboratoryTestTypeSummaryDto from(LaboratoryTestTypeRef ref) {
    return new LaboratoryTestTypeSummaryDto(ref.id(), ref.name());
  }
}
