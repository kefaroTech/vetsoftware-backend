package com.vetsoftware.app.company.domain;

public class CompanyHasActiveChildrenException extends RuntimeException {
    public CompanyHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete company " + id + ": has active " + childType + " children");
    }
}
