package com.vetsoftware.app.country.domain;

public class CountryHasActiveChildrenException extends RuntimeException {
    public CountryHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete country " + id + ": has active " + childType + " children");
    }
}
