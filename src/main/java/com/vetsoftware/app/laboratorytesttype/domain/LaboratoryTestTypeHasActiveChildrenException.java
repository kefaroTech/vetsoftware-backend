package com.vetsoftware.app.laboratorytesttype.domain;

public class LaboratoryTestTypeHasActiveChildrenException extends RuntimeException {
    public LaboratoryTestTypeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete laboratorytesttype " + id + ": has active " + childType + " children");
    }
}
