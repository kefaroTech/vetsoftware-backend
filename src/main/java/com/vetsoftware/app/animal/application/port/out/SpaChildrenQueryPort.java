package com.vetsoftware.app.animal.application.port.out;

public interface SpaChildrenQueryPort {
    boolean existsActiveByAnimalId(Long parentId);
}
