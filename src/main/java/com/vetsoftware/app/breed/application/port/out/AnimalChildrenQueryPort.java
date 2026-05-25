package com.vetsoftware.app.breed.application.port.out;

public interface AnimalChildrenQueryPort {
    boolean existsActiveByBreedId(Long parentId);
}
