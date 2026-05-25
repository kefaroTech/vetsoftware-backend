package com.vetsoftware.app.diagnosticimagingtype.domain;

public class DiagnosticImagingTypeHasActiveChildrenException extends RuntimeException {
    public DiagnosticImagingTypeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete diagnosticimagingtype " + id + ": has active " + childType + " children");
    }
}
