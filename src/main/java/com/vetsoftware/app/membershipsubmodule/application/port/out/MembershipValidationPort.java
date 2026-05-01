package com.vetsoftware.app.membershipsubmodule.application.port.out;

public interface MembershipValidationPort {
    void validateExists(Long membershipId);
}
