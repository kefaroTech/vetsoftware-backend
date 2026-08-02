package com.vetsoftware.app.cashregister.application.port.out;

/**
 * Política por empresa: ¿se exige una caja OPEN para poder cobrar (POS/abono)?
 * Flag {@code
 * cashregister.required}.
 */
public interface CashRequiredPolicyPort {
    boolean isCashRequired(Long companyId);
}
