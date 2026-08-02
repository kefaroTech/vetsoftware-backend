package com.vetsoftware.app.rolepermission.application.dto;

import com.vetsoftware.app.rolepermission.domain.PermissionRef;

public record PermissionSummaryDto(Long id, String name, String code) {
  public static PermissionSummaryDto from(PermissionRef ref) {
    return new PermissionSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
