package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.Quote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La cotizacion proyectada hacia fuera.
 *
 * <p>
 * Los cuatro totales salen GUARDADOS de la cabecera y las lineas salen de sus
 * copias congeladas. Nada de esto se recalcula al pintar: es todo el punto del
 * documento.
 */
public record QuoteDto(Long id, String quoteNumber, CompanySummaryDto company, String prospectName,
        String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
        String billingCycle, BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal taxAmount, BigDecimal totalAmount, String status, LocalDate validUntil,
        int trialDays, LocalDateTime acceptedAt, String acceptedByEmail, String acceptedIp,
        String clientRequestId, List<QuoteLineDto> lines, LocalDateTime createdDate,
        boolean enabled) {

    public static QuoteDto from(Quote quote) {
        return new QuoteDto(quote.getId(), quote.getQuoteNumber(),
                CompanySummaryDto.from(quote.getCompany()), quote.getProspectName(),
                quote.getProspectEmail(), quote.getProspectDocument(), quote.getProspectPhone(),
                quote.getPriceListId(), quote.getBillingCycle().name(), quote.getSubtotalAmount(),
                quote.getDiscountAmount(), quote.getTaxAmount(), quote.getTotalAmount(),
                quote.getStatus().name(), quote.getValidUntil(), quote.getTrialDays(),
                quote.getAcceptedAt(), quote.getAcceptedByEmail(), quote.getAcceptedIp(),
                quote.getClientRequestId(),
                quote.getLines().stream().map(QuoteLineDto::from).toList(), quote.getCreatedDate(),
                quote.isEnabled());
    }
}
