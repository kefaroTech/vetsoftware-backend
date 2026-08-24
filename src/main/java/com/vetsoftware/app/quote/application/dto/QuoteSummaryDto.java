package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.QuoteSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** La cabecera de la cotizacion para los listados. Sin lineas ni respuestas. */
public record QuoteSummaryDto(Long id, String quoteNumber, CompanySummaryDto company,
        String prospectName, String prospectEmail, Long priceListId, String billingCycle,
        BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, String status, LocalDate validUntil, int trialDays,
        LocalDateTime acceptedAt, LocalDateTime createdDate, boolean enabled) {

    public static QuoteSummaryDto from(QuoteSummary summary) {
        return new QuoteSummaryDto(summary.id(), summary.quoteNumber(),
                CompanySummaryDto.from(summary.company()), summary.prospectName(),
                summary.prospectEmail(), summary.priceListId(), summary.billingCycle().name(),
                summary.subtotalAmount(), summary.discountAmount(), summary.taxAmount(),
                summary.totalAmount(), summary.status().name(), summary.validUntil(),
                summary.trialDays(), summary.acceptedAt(), summary.createdDate(),
                summary.enabled());
    }
}
