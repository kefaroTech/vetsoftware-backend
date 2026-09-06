package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import java.util.List;

/**
 * Documentos {@code RECURRING_CYCLE} con saldo, sin pago pendiente y sin ningún
 * intento todavía: la primera vez que se intenta cobrar cada uno.
 *
 * <p>
 * Barrido de plataforma sin empresa delante —por diseño, no por descuido—: el
 * único caso de uso que lo consume, {@code RunPaymentCollectionUseCase}, está
 * cerrado a {@code hasRole('SYSTEM')}
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface NewRecurringChargeQueryPort {
    List<RecurringChargeTarget> findAfter(long afterId, int batchSize);
}
