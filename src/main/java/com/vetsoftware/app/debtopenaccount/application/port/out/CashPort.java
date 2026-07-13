package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;

/**
 * Afecta la caja (feature {@code cashregister}) por un abono a una cuenta abierta. El abono entra en la sesión OPEN de
 * la sede de la cuenta (OPEN_ACCOUNT_IN, ref {@code OPEN_ACCOUNT_PAYMENT} + id del abono), idempotente por abono+medio;
 * al anular el abono se compensa con VOID_OUT. El adapter resuelve la sede de la cuenta y mapea el medio de pago.
 */
public interface CashPort {

    /**
     * Bloqueo "caja requerida": si la empresa lo exige y la sede de la cuenta no tiene caja OPEN, lanza
     * {@code NoOpenCashSessionException} (→ 409 NO_OPEN_CASH_SESSION) ANTES de registrar el abono. No-op si no se exige.
     */
    void requireOpenSession(Long companyId, Long openAccountId);

    /** Registra el abono en la caja OPEN de la sede de la cuenta. Idempotente. No-op si no hay caja abierta. */
    void registerPayment(Long companyId, Long openAccountId, Long paymentId, PaymentMethod method, BigDecimal amount,
                         Long employeeId);

    /** Compensa (VOID_OUT) en la caja OPEN actual el abono anulado. Idempotente. No-op si no hay caja abierta. */
    void reversePayment(Long companyId, Long openAccountId, Long paymentId, PaymentMethod method, BigDecimal amount,
                        Long actorId);
}
