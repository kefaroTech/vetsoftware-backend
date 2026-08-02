package com.vetsoftware.app.openaccount.application.dto;

import com.vetsoftware.app.openaccount.domain.OwnerRef;

public record OwnerSummaryDto(Long id, String name, String document) {
  public static OwnerSummaryDto from(OwnerRef owner) {
    return new OwnerSummaryDto(owner.id(), owner.name(), owner.document());
  }
}
