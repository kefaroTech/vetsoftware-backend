package com.vetsoftware.app.generalchargeopenaccount.application.dto;

import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;

public record OpenAccountSummaryDto(Long id, Long companyId) {
    public static OpenAccountSummaryDto from(OpenAccountRef openAccount) {
        return new OpenAccountSummaryDto(openAccount.id(), openAccount.companyId());
    }
}
