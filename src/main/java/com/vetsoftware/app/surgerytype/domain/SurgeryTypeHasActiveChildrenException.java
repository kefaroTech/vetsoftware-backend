package com.vetsoftware.app.surgerytype.domain;

public class SurgeryTypeHasActiveChildrenException extends RuntimeException {
    public SurgeryTypeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete surgerytype " + id + ": has active " + childType + " children");
    }
}
