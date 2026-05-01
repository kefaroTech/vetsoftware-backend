package com.vetsoftware.app.membershipsubmodule.domain;

public class MembershipSubModuleNotFoundException extends RuntimeException {
    public MembershipSubModuleNotFoundException(Long id) {
        super("MembershipSubModule not found: " + id);
    }
}
