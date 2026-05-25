package com.vetsoftware.app.membership.domain;

public class MembershipHasActiveChildrenException extends RuntimeException {
    public MembershipHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete membership " + id + ": has active " + childType + " children");
    }
}
