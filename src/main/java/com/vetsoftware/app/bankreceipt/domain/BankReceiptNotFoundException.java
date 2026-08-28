package com.vetsoftware.app.bankreceipt.domain;

public class BankReceiptNotFoundException extends RuntimeException {

    public BankReceiptNotFoundException(Long id) {
        super("Bank receipt not found: " + id);
    }
}
