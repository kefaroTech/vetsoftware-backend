package com.vetsoftware.app.baserolepermission.application.dto;

import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;

public record BaseRoleSummaryDto(Long id, String name, String code) {
  public static BaseRoleSummaryDto from(BaseRoleRef ref) {
    return new BaseRoleSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
