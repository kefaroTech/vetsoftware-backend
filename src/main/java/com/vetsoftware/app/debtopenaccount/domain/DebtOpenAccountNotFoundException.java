package com.vetsoftware.app.debtopenaccount.domain;

public class DebtOpenAccountNotFoundException extends RuntimeException {
    public DebtOpenAccountNotFoundException(Long id) {
        super("DebtOpenAccount not found: " + id);
    }
}
