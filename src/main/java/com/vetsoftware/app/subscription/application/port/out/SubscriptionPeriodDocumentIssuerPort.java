package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.IssuedPeriodDocument;
import java.time.LocalDate;

/**
 * Emite, DENTRO de la misma transaccion de la firma, el documento de cobro del
 * primer periodo del contrato.
 *
 * <p>
 * Idempotente por construccion: {@code IssueSubscriptionPeriodDocumentUseCase}
 * devuelve el documento ya existente si el periodo ya se facturo, asi que un
 * reintento de la aceptacion no duplica nada.
 *
 * <p>
 * {@code null} cuando el periodo no genero ningun documento (sin cargos
 * pendientes).
 */
public interface SubscriptionPeriodDocumentIssuerPort {

    IssuedPeriodDocument issueFirstPeriod(Long companyId, Long subscriptionId,
            LocalDate periodStart, LocalDate periodEnd);
}
