package com.vetsoftware.app.debtopenaccount.application.dto;

import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;

public record OpenAccountSummaryDto(Long id, Long companyId) {
  public static OpenAccountSummaryDto from(OpenAccountRef openAccount) {
    return new OpenAccountSummaryDto(openAccount.id(), openAccount.companyId());
  }
}
