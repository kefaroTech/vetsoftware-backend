package com.vetsoftware.app.spatype.domain;

public class SpaTypeHasActiveChildrenException extends RuntimeException {
    public SpaTypeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete spatype " + id + ": has active " + childType + " children");
    }
}
