package com.vetsoftware.app.tax.domain;

public class TaxHasActiveChildrenException extends RuntimeException {
    public TaxHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete tax " + id + ": has active " + childType + " children");
    }
}
