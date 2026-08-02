package com.vetsoftware.app.service.application.dto;

import com.vetsoftware.app.service.domain.TaxRef;
import java.math.BigDecimal;

public record TaxSummaryDto(Long id, String name, BigDecimal percentage) {
  public static TaxSummaryDto from(TaxRef ref) {
    return new TaxSummaryDto(ref.id(), ref.name(), ref.percentage());
  }
}
