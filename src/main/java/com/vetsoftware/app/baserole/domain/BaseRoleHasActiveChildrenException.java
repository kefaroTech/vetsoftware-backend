package com.vetsoftware.app.baserole.domain;

public class BaseRoleHasActiveChildrenException extends RuntimeException {
    public BaseRoleHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete baserole " + id + ": has active " + childType + " children");
    }
}
