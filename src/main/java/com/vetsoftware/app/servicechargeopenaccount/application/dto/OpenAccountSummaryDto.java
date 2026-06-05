package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;

public record OpenAccountSummaryDto(Long id, Long companyId) {
    public static OpenAccountSummaryDto from(OpenAccountRef openAccount) {
        return new OpenAccountSummaryDto(openAccount.id(), openAccount.companyId());
    }
}
