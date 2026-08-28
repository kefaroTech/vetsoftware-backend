package com.vetsoftware.app.accountingexport.domain;

public class AccountingExportNotFoundException extends RuntimeException {

    public AccountingExportNotFoundException(Long id) {
        super("Accounting export not found: " + id);
    }
}
