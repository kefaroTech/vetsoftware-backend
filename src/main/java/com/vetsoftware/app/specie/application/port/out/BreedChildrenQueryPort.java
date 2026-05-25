package com.vetsoftware.app.specie.application.port.out;

public interface BreedChildrenQueryPort {
    boolean existsActiveBySpecieId(Long parentId);
}
