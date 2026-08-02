package com.vetsoftware.app.animalcolor.application.port.out;

public interface AnimalChildrenQueryPort {
  boolean existsActiveByAnimalColorId(Long parentId);
}
