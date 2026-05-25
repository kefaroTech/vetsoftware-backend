package com.vetsoftware.app.basepermission.domain;

public class BasePermissionHasActiveChildrenException extends RuntimeException {
    public BasePermissionHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete basepermission " + id + ": has active " + childType + " children");
    }
}
