package com.vetsoftware.app.accountmapping.domain;

public class AccountMappingNotFoundException extends RuntimeException {

    public AccountMappingNotFoundException(Long id) {
        super("Account mapping not found: " + id);
    }
}
