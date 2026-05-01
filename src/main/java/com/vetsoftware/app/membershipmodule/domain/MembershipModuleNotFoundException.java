package com.vetsoftware.app.membershipmodule.domain;

public class MembershipModuleNotFoundException extends RuntimeException {
    public MembershipModuleNotFoundException(Long id) {
        super("MembershipModule not found: " + id);
    }
}
