package com.vetsoftware.app.animal.application.port.out;

public interface HospitalizationChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
