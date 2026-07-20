package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;

/**
 * Afecta la caja (feature {@code cashregister}) por un abono a una cuenta abierta. El abono entra en la sesión OPEN de
 * la caja OPEN del empleado en la sede de la cuenta (OPEN_ACCOUNT_IN, ref {@code OPEN_ACCOUNT_PAYMENT} + id del
 * abono), idempotente por abono+medio; al anular el abono se compensa con VOID_OUT en la caja del actor.
 */
public interface CashPort {

    /**
     * Exige que el empleado tenga su propia caja OPEN en la sede de la cuenta antes de cobrar o anular.
     */
    void requireOpenSession(Long companyId, Long openAccountId, Long employeeId);

    /** Registra el abono en la caja OPEN del empleado en la sede de la cuenta. Idempotente. */
    void registerPayment(Long companyId, Long openAccountId, Long paymentId, PaymentMethod method, BigDecimal amount,
                         Long employeeId);

    /** Compensa (VOID_OUT) en la caja OPEN del actor en la sede de la cuenta. Idempotente. */
    void reversePayment(Long companyId, Long openAccountId, Long paymentId, PaymentMethod method, BigDecimal amount,
                        Long actorId);
}
