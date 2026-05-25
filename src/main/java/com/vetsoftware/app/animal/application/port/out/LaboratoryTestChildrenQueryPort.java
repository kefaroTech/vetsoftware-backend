package com.vetsoftware.app.animal.application.port.out;

public interface LaboratoryTestChildrenQueryPort {
    boolean existsActiveByAnimalId(Long parentId);
}
