package com.vetsoftware.app.quote.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La cotizacion completa: cabecera con totales guardados y lineas.
 */
public record QuoteResponse(Long id, String quoteNumber, CompanySummary company,
        String prospectName, String prospectEmail, String prospectDocument, String prospectPhone,
        Long priceListId, String billingCycle, BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal taxAmount, BigDecimal totalAmount, String status, LocalDate validUntil,
        int trialDays, LocalDateTime acceptedAt, String acceptedByEmail, String acceptedIp,
        String clientRequestId, List<QuoteLineResponse> lines, LocalDateTime createdDate,
        boolean enabled) {
}
