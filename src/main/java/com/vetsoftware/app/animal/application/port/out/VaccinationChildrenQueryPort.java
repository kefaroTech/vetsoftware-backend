package com.vetsoftware.app.animal.application.port.out;

public interface VaccinationChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
