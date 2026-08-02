package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.OwnerRef;

public record OwnerSummaryDto(Long id, String name, String document) {
  public static OwnerSummaryDto from(OwnerRef ref) {
    return new OwnerSummaryDto(ref.id(), ref.name(), ref.document());
  }
}
