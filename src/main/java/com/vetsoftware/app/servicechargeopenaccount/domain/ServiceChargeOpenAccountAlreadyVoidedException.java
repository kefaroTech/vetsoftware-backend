package com.vetsoftware.app.servicechargeopenaccount.domain;

public class ServiceChargeOpenAccountAlreadyVoidedException extends RuntimeException {
    public ServiceChargeOpenAccountAlreadyVoidedException(Long id) {
        super("Service charge open account already voided: " + id);
    }
}
