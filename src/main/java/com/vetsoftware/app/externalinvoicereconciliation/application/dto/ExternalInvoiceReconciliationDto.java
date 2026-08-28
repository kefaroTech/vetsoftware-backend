package com.vetsoftware.app.externalinvoicereconciliation.application.dto;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La conciliacion tal como sale de la capa de aplicacion.
 *
 * <p>
 * Lleva los <strong>cuatro numeros enfrentados</strong> y no solo la
 * diferencia: quien mira un descuadre necesita ver si la base cuadra y el
 * impuesto no -calculo- o si no cuadra ninguno de los dos -base-. Publicar solo
 * {@code difference} obligaria a abrir otra pantalla para responder la unica
 * pregunta que importa.
 */
public record ExternalInvoiceReconciliationDto(Long id, Long companyId, Long billingDocumentId,
        String externalResolutionNumber, Integer externalRangeFrom, Integer externalRangeTo,
        LocalDate resolutionValidUntil, String externalInvoiceId, String externalCufe,
        BigDecimal computedTotal, BigDecimal computedTax, BigDecimal externalTotal,
        BigDecimal externalTax, BigDecimal difference, ExternalInvoiceReconciliationStatus status,
        Long resolvedBySystemUserId, LocalDateTime resolvedAt, String resolutionNote,
        String postingPeriod, LocalDateTime createdDate) {

    public static ExternalInvoiceReconciliationDto from(ExternalInvoiceReconciliation entity) {
        return new ExternalInvoiceReconciliationDto(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getExternalResolutionNumber(),
                entity.getExternalRangeFrom(), entity.getExternalRangeTo(),
                entity.getResolutionValidUntil(), entity.getExternalInvoiceId(),
                entity.getExternalCufe(), entity.getComputedTotal(), entity.getComputedTax(),
                entity.getExternalTotal(), entity.getExternalTax(), entity.getDifference(),
                entity.getStatus(), entity.getResolvedBySystemUserId(), entity.getResolvedAt(),
                entity.getResolutionNote(), entity.getPostingPeriod(), entity.getCreatedDate());
    }
}
