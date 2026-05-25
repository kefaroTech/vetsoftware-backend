package com.vetsoftware.app.systempermission.domain;

public class SystemPermissionHasActiveChildrenException extends RuntimeException {
    public SystemPermissionHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete systempermission " + id + ": has active " + childType + " children");
    }
}
