package com.vetsoftware.app.membershipsubmodule.application.dto;

import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;

public record SubModuleSummaryDto(Long id, String name, String code) {
  public static SubModuleSummaryDto from(SubModuleRef ref) {
    return new SubModuleSummaryDto(ref.id(), ref.name(), ref.code());
  }
}
