package com.vetsoftware.app.customercredit.domain;

public class CustomerCreditEntryNotFoundException extends RuntimeException {
    public CustomerCreditEntryNotFoundException(Long id) {
        super("CustomerCreditEntry not found: " + id);
    }
}
