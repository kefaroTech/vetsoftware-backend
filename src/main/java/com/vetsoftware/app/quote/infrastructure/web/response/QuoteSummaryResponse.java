package com.vetsoftware.app.quote.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La cabecera para los listados. Sin lineas ni respuestas: los cuatro totales
 * estan guardados en la cabecera, asi que un embudo comercial completo se pinta
 * sin tocar una sola linea.
 */
public record QuoteSummaryResponse(Long id, String quoteNumber, CompanySummary company,
        String prospectName, String prospectEmail, Long priceListId, String billingCycle,
        BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, String status, LocalDate validUntil, int trialDays,
        LocalDateTime acceptedAt, LocalDateTime createdDate, boolean enabled) {
}
