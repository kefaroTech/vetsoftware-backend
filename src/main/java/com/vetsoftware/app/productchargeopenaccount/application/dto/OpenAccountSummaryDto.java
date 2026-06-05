package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;

public record OpenAccountSummaryDto(Long id, Long companyId) {
    public static OpenAccountSummaryDto from(OpenAccountRef openAccount) {
        return new OpenAccountSummaryDto(openAccount.id(), openAccount.companyId());
    }
}
