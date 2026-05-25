package com.vetsoftware.app.consultation.domain;

public class ConsultationHasActiveChildrenException extends RuntimeException {
    public ConsultationHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete consultation " + id + ": has active " + childType + " children");
    }
}
