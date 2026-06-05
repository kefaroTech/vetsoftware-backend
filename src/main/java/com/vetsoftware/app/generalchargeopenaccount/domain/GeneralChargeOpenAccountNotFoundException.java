package com.vetsoftware.app.generalchargeopenaccount.domain;

public class GeneralChargeOpenAccountNotFoundException extends RuntimeException {
    public GeneralChargeOpenAccountNotFoundException(Long id) {
        super("GeneralChargeOpenAccount not found: " + id);
    }
}
