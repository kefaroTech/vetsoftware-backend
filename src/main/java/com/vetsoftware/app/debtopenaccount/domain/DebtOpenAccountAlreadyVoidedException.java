package com.vetsoftware.app.debtopenaccount.domain;

public class DebtOpenAccountAlreadyVoidedException extends RuntimeException {
    public DebtOpenAccountAlreadyVoidedException(Long id) {
        super("Debt open account payment already voided: " + id);
    }
}
