package com.vetsoftware.app.consultationtype.domain;

public class ConsultationTypeHasActiveChildrenException extends RuntimeException {
    public ConsultationTypeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete consultationtype " + id + ": has active " + childType + " children");
    }
}
