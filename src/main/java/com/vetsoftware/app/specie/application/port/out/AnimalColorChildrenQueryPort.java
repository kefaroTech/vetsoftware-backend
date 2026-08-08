package com.vetsoftware.app.specie.application.port.out;

public interface AnimalColorChildrenQueryPort {
    boolean existsActiveBySpecieId(Long parentId);
}
