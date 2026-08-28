package com.vetsoftware.app.companytrialwindow.application.dto;

import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** El reloj de la empresa tal como sale de la feature. */
public record CompanyTrialWindowDto(Long id, Long companyId, LocalDate startDate, LocalDate endDate,
        int windowDays, Long sourceQuoteId, LocalDateTime closedAt, boolean open) {

    public static CompanyTrialWindowDto from(CompanyTrialWindow window) {
        return new CompanyTrialWindowDto(window.getId(), window.getCompanyId(),
                window.getStartDate(), window.getEndDate(), window.getWindowDays(),
                window.getSourceQuoteId(), window.getClosedAt(), window.isOpen());
    }
}
