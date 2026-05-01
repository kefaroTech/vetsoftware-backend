package com.vetsoftware.app.membershipmodule.application.port.out;

public interface MembershipValidationPort {
    void validateExists(Long membershipId);
}
