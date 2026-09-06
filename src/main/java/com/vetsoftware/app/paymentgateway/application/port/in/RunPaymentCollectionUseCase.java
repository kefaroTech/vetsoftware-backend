package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido diario de cobro recurrente: uno de los barridos de plataforma.
 *
 * <p>
 * Dos fuentes de trabajo, cada una con su propia paginación, y por eso dos
 * métodos y no uno: los documentos nuevos se recorren por cursor de id
 * ({@link #collectNewChargesAfter}) y los reintentos vencidos por la página de
 * {@code ListDuePaymentAttemptsUseCase} ({@link #collectDueRetries}). Ninguno
 * de los dos filtra por empresa —son barridos cross-tenant, cada uno con su
 * puerto propio declarado como tal—, de ahí {@code hasRole('SYSTEM')} a secas
 * en los dos ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface RunPaymentCollectionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PaymentCollectionBatchResult collectNewChargesAfter(long afterId, int batchSize);

    @PreAuthorize("hasRole('SYSTEM')")
    PaymentCollectionBatchResult collectDueRetries(LocalDateTime now, int page, int pageSize);
}
