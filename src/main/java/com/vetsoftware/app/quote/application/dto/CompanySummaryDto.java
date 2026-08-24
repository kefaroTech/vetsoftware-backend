package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.CompanyRef;

/**
 * Empresa destinataria proyectada; null mientras la cotizacion sea a prospecto.
 */
public record CompanySummaryDto(Long id, String name, String identifier) {
    public static CompanySummaryDto from(CompanyRef ref) {
        return ref == null ? null : new CompanySummaryDto(ref.id(), ref.name(), ref.identifier());
    }
}
