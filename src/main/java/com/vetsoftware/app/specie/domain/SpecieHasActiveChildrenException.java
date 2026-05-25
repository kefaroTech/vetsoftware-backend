package com.vetsoftware.app.specie.domain;

public class SpecieHasActiveChildrenException extends RuntimeException {
    public SpecieHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete specie " + id + ": has active " + childType + " children");
    }
}
