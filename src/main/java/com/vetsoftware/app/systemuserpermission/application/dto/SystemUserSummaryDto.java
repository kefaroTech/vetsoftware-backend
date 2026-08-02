package com.vetsoftware.app.systemuserpermission.application.dto;

import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;

public record SystemUserSummaryDto(Long id, String code) {
  public static SystemUserSummaryDto from(SystemUserRef ref) {
    return new SystemUserSummaryDto(ref.id(), ref.code());
  }
}
