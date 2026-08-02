package com.vetsoftware.app.spa.application.dto;

import com.vetsoftware.app.spa.domain.SpaTypeRef;

public record SpaTypeSummaryDto(Long id, String name) {
  public static SpaTypeSummaryDto from(SpaTypeRef ref) {
    return new SpaTypeSummaryDto(ref.id(), ref.name());
  }
}
