package com.vetsoftware.app.accountingaccount.domain;

public class AccountingAccountNotFoundException extends RuntimeException {

    public AccountingAccountNotFoundException(Long id) {
        super("Accounting account not found: " + id);
    }

    public AccountingAccountNotFoundException(String code) {
        super("Accounting account not found: " + code);
    }
}
