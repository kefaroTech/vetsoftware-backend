package com.vetsoftware.app.baserolepermission.application.dto;

import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;

public record BasePermissionSummaryDto(Long id, String name, String code) {
  public static BasePermissionSummaryDto from(BasePermissionRef ref) {
    return new BasePermissionSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
